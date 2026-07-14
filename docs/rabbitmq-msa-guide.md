# MSA RabbitMQ 메시징 가이드

## 개요

BDS 백엔드는 서비스 간 이벤트를 RabbitMQ를 통해 교환합니다.  
공통 설정은 `modules/messaging`, 이벤트 타입 정의는 `libs/bds-events`에 모아 두고  
각 서비스는 이를 의존성으로 가져다 사용하는 구조입니다.

---

## 아키텍처 흐름

```
[Producer Service]
  │
  ├─ DirectEventPublisher.publish(event)       ← @PublishTo 어노테이션으로 목적지 선언
  │         │
  │   RabbitTemplate (JacksonJsonMessageConverter)
  │         │
  │    order.exchange ──── routing key ───▶ [Consumer Queue]
  │                                                │
  │                                    @RabbitListener + ProcessedEventStore
  │                                                │
  │                                        [Consumer Service]
  │                                         비즈니스 로직 처리
  │
  └─ (유실 불가 이벤트) Spring Modulith @Externalized    ← 현재 미적용, JDBC 백엔드 필요
```

### 큐 구조 (`BdsQueues.workQueue`)

```
[source exchange] ──routingKey──▶ [main queue (quorum)]
                                       │ x-delivery-limit: 5 초과 시
                                       ▼
                                   [dlx.exchange] ──▶ [queue.dlq]
```

- 모든 큐는 **Quorum Queue** (고가용성)
- 재시도 5회 초과 시 자동으로 DLQ로 이동

---

## 신규 서비스 적용 방법

### 1. `build.gradle` 의존성 추가

```gradle
dependencies {
    implementation project(':libs:bds-events')     // 이벤트 타입 + @PublishTo
    implementation project(':modules:messaging')   // RabbitMQ 공통 설정
    // ... 기존 의존성
}
```

### 2. `application-local.yml` / `application-dev.yml` RabbitMQ 설정 추가

#### 발행(Producer) 서비스

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    virtual-host: msa
    username: ${RABBITMQ_USER:your-service}
    password: ${RABBITMQ_PASSWORD:your1234}
    # 발행 서비스 필수 — confirm 콜백이 동작하기 위한 설정
    publisher-confirm-type: correlated
    publisher-returns: true

# messaging 모듈 스케줄러 주기 (기본값 사용 시 생략 가능)
bds:
  messaging:
    events:
      republish-delay: 30s
      republish-min-age: 1m
      retention: 7d
```

#### 수신(Consumer) 서비스

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    virtual-host: msa
    username: ${RABBITMQ_USER:your-service}
    password: ${RABBITMQ_PASSWORD:your1234}
    # 수신 전용 서비스는 confirm 설정 불필요
```

---

### 3. `RabbitTopologyConfig` 작성

`modules/messaging`의 `BdsRabbitAutoConfig`는 `@AutoConfiguration`이므로 사용자 설정보다 늦게 로드됩니다.  
따라서 각 서비스의 `@Configuration`에서 `MessageConverter`와 `RabbitTemplate`을 직접 선언해야 합니다.

```java
@Configuration
public class RabbitTopologyConfig {

    // 1. JSON 컨버터 선언 (BdsRabbitAutoConfig의 @ConditionalOnMissingBean이 중복 방지)
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    // 2. 표준 템플릿 (JSON 컨버터 + mandatory + confirm/returns 로깅 적용)
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf,
                                         MessageConverter converter,
                                         ObjectProvider<RabbitTemplateCustomizer> customizers) {
        return BdsRabbit.standardTemplate(cf, converter, customizers);
    }

    // 3. 이 서비스가 소유한 Exchange 선언 (발행 서비스만)
    @Bean
    public TopicExchange myExchange() {
        return ExchangeBuilder.topicExchange("my.exchange").durable(true).build();
    }
}
```

---

### 4. 이벤트 발행 (Producer)

#### 이벤트 정의 (`libs/bds-events`)

```java
@PublishTo(exchange = "order.exchange", routingKey = "order.created")
public record OrderCreatedDirectEvent(
        UUID eventId,
        Long orderId,
        Long amount,
        Instant occurredAt
) {
    public static OrderCreatedDirectEvent of(Long orderId, Long amount) {
        return new OrderCreatedDirectEvent(UUID.randomUUID(), orderId, amount, Instant.now());
    }
}
```

#### 발행

```java
@Component
@RequiredArgsConstructor
public class MyEventPublisher {
    private final DirectEventPublisher directEventPublisher;

    public void publishOrderCreated(Long orderId, Long amount) {
        directEventPublisher.publish(OrderCreatedDirectEvent.of(orderId, amount));
        // @PublishTo의 exchange/routingKey를 자동으로 읽어 발행
    }
}
```

---

### 5. 이벤트 수신 (Consumer)

#### 큐 선언

```java
@Configuration
public class MyQueues {
    public static final String ORDER_CREATED = "my-service.order-created.queue";

    @Bean
    public Declarables orderCreatedQueue() {
        // BdsQueues.workQueue(큐이름, exchange, routingKey)
        // → 메인 큐 + DLQ + 바인딩 일체 선언
        return BdsQueues.workQueue(ORDER_CREATED, "order.exchange", "order.created");
    }
}
```

#### 리스너 작성

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedListener {
    private final ProcessedEventStore processedEventStore;

    @RabbitListener(queues = MyQueues.ORDER_CREATED)
    public void handle(OrderCreatedDirectEvent event) {
        if (!processedEventStore.markProcessed(event.eventId())) {
            log.info("중복 이벤트 스킵: {}", event.eventId());
            return;  // 정상 종료 → ack (재처리 방지)
        }
        // 비즈니스 로직
        log.info("OrderCreated 수신: orderId={}, amount={}", event.orderId(), event.amount());
    }
}
```

---

### 6. `ProcessedEventStore` 구현 (멱등성)

`ProcessedEventStore`는 동일 이벤트의 중복 처리를 막는 인터페이스입니다.  
구현 여부와 방식은 서비스의 비즈니스 요구사항에 따라 선택합니다.

```java
public interface ProcessedEventStore {
    // 처음 처리하는 eventId면 true 반환 (처리 진행)
    // 이미 처리한 eventId면 false 반환 (스킵)
    boolean markProcessed(UUID eventId);
}
```

#### 구현체 선택 기준

| 상황 | 구현체 | 설명 |
|------|--------|------|
| 단일 인스턴스 / 재시작 시 중복 허용 | `InMemoryProcessedEventStore` | `ConcurrentHashMap` 사용, 가장 단순 |
| 다중 인스턴스 / 재시작 후도 중복 방지 | `RedisProcessedEventStore` (직접 구현) | Redis `SET NX` 활용 |
| 멱등성 검증 불필요 | 항상 `true` 반환하는 더미 구현체 | 비즈니스 로직 자체가 멱등일 때 |

#### `InMemoryProcessedEventStore` 동작 원리

```java
@Component
public class InMemoryProcessedEventStore implements ProcessedEventStore {
    private final ConcurrentHashMap<UUID, Boolean> processed = new ConcurrentHashMap<>();

    @Override
    public boolean markProcessed(UUID eventId) {
        // putIfAbsent: key가 없으면 삽입 후 null 반환 → 첫 처리 → true
        // putIfAbsent: key가 있으면 삽입 안 하고 기존 값 반환 → 중복 → false
        return processed.putIfAbsent(eventId, Boolean.TRUE) == null;
    }
}
```

- `ConcurrentHashMap.putIfAbsent()`는 atomic하게 동작하므로 멀티스레드 안전
- 단, 서버 재시작 시 맵이 초기화되므로 재시작 전후 중복 수신이 발생할 수 있음

---

### 7. CI workflow 수정

`ci-your-service.yml`의 `sparse-checkout`과 `Start MSA RabbitMQ` 스텝을 추가합니다.

```yaml
steps:
  - uses: actions/checkout@v4
    with:
      sparse-checkout: |
        services/your-service
        modules/common
        modules/messaging      # 추가
        libs/bds-events        # 추가
        build.gradle
        settings.gradle
        gradlew
        gradle

  - name: Start MSA RabbitMQ
    run: |
      docker run -d --name rabbitmq-msa \
        -p 5672:5672 \
        -e RABBITMQ_DEFAULT_USER=your-service \
        -e RABBITMQ_DEFAULT_PASS=your1234 \
        -e RABBITMQ_DEFAULT_VHOST=msa \
        rabbitmq:4

  - name: Wait for RabbitMQ
    run: |
      for i in $(seq 1 30); do
        if docker exec rabbitmq-msa rabbitmq-diagnostics -q ping > /dev/null 2>&1; then
          echo "rabbitmq-msa ready"; break
        fi
        echo "Waiting... ($i/30)"; sleep 2
      done
      docker exec rabbitmq-msa rabbitmq-diagnostics -q ping || exit 1

  - name: Build and Test
    env:
      RABBITMQ_HOST: localhost
      RABBITMQ_PORT: 5672
      RABBITMQ_USER: your-service
      RABBITMQ_PASSWORD: your1234
    run: ./gradlew :services:your-service:clean :services:your-service:build
```

---

## 모듈 구조 요약

```
libs/bds-events/
  └─ com.bds.common.events/
       ├─ PublishTo.java              # 발행 목적지 선언 어노테이션
       └─ order/OrderCreatedDirectEvent.java  # 이벤트 레코드 예시

modules/messaging/
  └─ com.bds.messaging/
       ├─ BdsRabbit.java              # standardTemplate / standardListenerFactory 팩토리
       ├─ BdsRabbitAutoConfig.java    # MessageConverter + confirm/returns 콜백 자동 설정
       ├─ BdsQueues.java              # workQueue (메인 큐 + DLQ 세트) 선언 헬퍼
       ├─ BdsEventPublicationAutoConfig.java  # 미완료 이벤트 재발행 스케줄러 (Modulith용)
       └─ idempotency/ProcessedEventStore.java  # 멱등성 인터페이스
```
