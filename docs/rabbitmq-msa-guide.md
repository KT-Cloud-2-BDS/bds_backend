# MSA RabbitMQ 메시징 가이드

## 개요

BDS 백엔드는 서비스 간 이벤트를 RabbitMQ를 통해 교환합니다.  
공통 설정은 `modules/messaging`, 이벤트 타입 정의는 `libs/bds-events`에 모아 두고  
각 서비스는 이를 의존성으로 가져다 사용하는 구조입니다.

---

## 발행 패턴 비교

BDS 에서 사용 가능한 이벤트 발행 방식은 두 가지입니다.

| 구분 | Direct 발행 | Outbox 패턴 |
|------|-------------|-------------|
| 발행 클래스 | `DirectEventPublisher` | `ApplicationEventPublisher` |
| 이벤트 어노테이션 | `@PublishTo(exchange, routingKey)` | `@Externalized("exchange::routingKey")` |
| 유실 가능성 | TX commit 전 발행 → 롤백 시 메시지 남음 | TX commit 후 발행 → 원자성 보장 |
| DB 테이블 | 없음 | `event_publication` (Flyway V3) |
| 추가 의존성 | 없음 | `spring-modulith-*` 4종 |
| 권장 용도 | 유실 허용 가능한 알림성 이벤트 | 결제·주문 등 유실 불가 이벤트 |

> **Order Service는 Outbox 패턴을 사용합니다.**  
> 결제 연동처럼 TX 원자성이 필요한 이벤트에는 반드시 Outbox 패턴을 적용하세요.

---

## 아키텍처 흐름

### Direct 발행

```
[Producer Service]
  │
  └─ DirectEventPublisher.publish(event)
          │   @PublishTo → exchange / routingKey 자동 추출
          ▼
     RabbitTemplate.convertAndSend()
          │
     [order.exchange] ──routing key──▶ [payment.order-created.queue]
                                                │
                                    @RabbitListener + ProcessedEventStore
                                                │
                                        [Consumer Service]
```

### Outbox 패턴 (Spring Modulith)

```
[Producer Service]  ← @Transactional 메서드 내부
  │
  └─ ApplicationEventPublisher.publishEvent(OrderCreatedEvent)
          │
          ▼
  PersistentApplicationEventMulticaster
  ① event_publication 테이블에 PUBLISHED 상태로 저장 (동일 TX)
          │
          ▼  TX commit 후
  EventExternalizerModuleListener (afterCommit)
  ② RabbitTemplate.convertAndSend()
          │
     [order.exchange] ──routing key──▶ [payment.order-created.queue]
                                                │
                                    @RabbitListener + ProcessedEventStore
                                                │
                                        [Consumer Service]
  ③ event_publication.status → COMPLETED
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

## RabbitMQ 신규 서비스 유저 추가

신규 서비스를 RabbitMQ에 연결하려면 `definitions.json`에 유저를 추가하고 비밀번호 해시를 생성해야 합니다.

### 1. 비밀번호 해시 생성

RabbitMQ는 평문 비밀번호를 그대로 저장하지 않고 SHA-256 해시를 사용합니다.  
아래 명령어로 해시를 생성합니다.

```bash
docker run --rm rabbitmq:4 rabbitmqctl hash-password <평문비밀번호>
```

예시:
```bash
$ docker run --rm rabbitmq:4 rabbitmqctl hash-password chat1234
# 출력: 2TpEAVhROmPU9uKZanKsBIR8itaXt9ifkH2Y/cif2L1iihQ8
```

### 2. `infra/rabbitmq/definitions.json` 수정

생성된 해시를 플레이스홀더로 추가합니다.

```json
{
  "users": [
    { "name": "your-service", "password_hash": "__YOUR_SERVICE_HASH__",
      "hashing_algorithm": "rabbit_password_hashing_sha256", "tags": [] }
  ],
  "permissions": [
    { "user": "your-service", "vhost": "msa",
      "configure": ".*", "write": ".*", "read": ".*" }
  ]
}
```

### 3. `.env.local` / `.env.dev` 에 해시값 추가

```bash
# .env.local
YOUR_SERVICE_HASH=<1번에서 생성한 해시>
```

그리고 `docker-compose.yml`의 `definitions-init` sed 커맨드에도 치환 라인을 추가합니다.

```yaml
command: >
  sh -c 'sed
  -e "s|__ADMIN_HASH__|$$ADMIN_HASH|g"
  -e "s|__ORDER_HASH__|$$ORDER_HASH|g"
  -e "s|__CHAT_HASH__|$$CHAT_HASH|g"
  -e "s|__YOUR_SERVICE_HASH__|$$YOUR_SERVICE_HASH|g"   # 추가
  /input/definitions.json > /output/definitions.json'
```

### 4. 컨테이너 재기동

```bash
# local 환경
docker compose --env-file .env.local up -d
```

> 이미 컨테이너가 떠 있다면 유저 정보는 `mnesia` 데이터에 남아있으므로  
> `.data/rabbitmq` 디렉토리를 삭제 후 재기동해야 definitions.json이 반영됩니다.

---

## 신규 서비스 적용 방법 — Direct 발행

유실을 허용하는 알림성 이벤트에 사용합니다.

### 1. `build.gradle` 의존성 추가

```gradle
dependencies {
    implementation project(':libs:bds-events')
    implementation project(':modules:messaging')
}
```

### 2. `application-local.yml` / `application-dev.yml` RabbitMQ 설정

#### 발행(Producer) 서비스

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    virtual-host: msa
    username: ${RABBITMQ_USER:your-service}
    password: ${RABBITMQ_PASSWORD:your1234}
    publisher-confirm-type: correlated
    publisher-returns: true
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
```

### 3. `RabbitTopologyConfig` 작성

`BdsRabbitAutoConfig`(`@AutoConfiguration`)가 `MessageConverter`와 `RabbitTemplateCustomizer`를  
자동으로 등록하므로, 서비스에서는 **Exchange 선언만** 하면 됩니다.  
커스텀 `RabbitTemplate`이나 `MessageConverter`를 직접 선언하면 Spring Modulith의 externalization 경로를 방해하므로 **선언하지 마세요**.

```java
@Configuration
public class RabbitTopologyConfig {

    // 이 서비스가 소유한 Exchange 선언 (발행 서비스만)
    @Bean
    public TopicExchange myExchange() {
        return ExchangeBuilder.topicExchange("my.exchange").durable(true).build();
    }
}
```

### 4. 이벤트 정의 (`libs/bds-events`)

```java
@PublishTo(exchange = "my.exchange", routingKey = "my.event.occurred")
public record MyEvent(
        UUID eventId,
        Long targetId,
        Instant occurredAt
) {
    public static MyEvent of(Long targetId) {
        return new MyEvent(UUID.randomUUID(), targetId, Instant.now());
    }
}
```

### 5. 이벤트 발행

```java
@Component
@RequiredArgsConstructor
public class MyEventPublisher {
    private final DirectEventPublisher directEventPublisher;

    public void publish(Long targetId) {
        directEventPublisher.publish(MyEvent.of(targetId));
        // @PublishTo의 exchange/routingKey를 자동으로 읽어 발행
    }
}
```

### 6. 이벤트 수신 (Consumer)

```java
@Configuration
public class MyQueues {
    public static final String MY_EVENT = "my-service.my-event.queue";

    @Bean
    public Declarables myEventQueue() {
        return BdsQueues.workQueue(MY_EVENT, "my.exchange", "my.event.occurred");
    }
}
```

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class MyEventListener {
    private final ProcessedEventStore processedEventStore;

    @RabbitListener(queues = MyQueues.MY_EVENT)
    public void handle(MyEvent event) {
        if (!processedEventStore.markProcessed(event.eventId())) {
            log.info("중복 이벤트 스킵: {}", event.eventId());
            return;
        }
        log.info("MyEvent 수신: targetId={}", event.targetId());
        // 비즈니스 로직
    }
}
```

---

## 신규 서비스 적용 방법 — Outbox 패턴 (Spring Modulith)

TX commit 이후에 메시지를 발행하므로 **비즈니스 TX와 메시지 발행의 원자성이 보장**됩니다.  
결제·주문 등 유실이 허용되지 않는 이벤트에 사용합니다.

### 1. `build.gradle` 의존성 추가

```gradle
dependencies {
    implementation project(':libs:bds-events')
    implementation project(':modules:messaging')

    // Modulith outbox
    implementation 'org.springframework.modulith:spring-modulith-starter-core'   // PersistentApplicationEventMulticaster 등록
    implementation 'org.springframework.modulith:spring-modulith-events-amqp'    // AMQP externalization 엔진
    implementation 'org.springframework.modulith:spring-modulith-events-jpa'     // JPA publication store
    implementation 'org.springframework.modulith:spring-modulith-events-jackson' // 이벤트 JSON 직렬화

    // JPA, Flyway (이미 있다면 생략)
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-flyway'
}
```

### 2. Flyway 마이그레이션 추가

`src/main/resources/db/migration/V{N}__event_publication.sql`을 추가합니다.

```sql
CREATE TABLE event_publication
(
    id                     UUID                        NOT NULL,
    listener_id            VARCHAR(255)                NOT NULL,
    event_type             VARCHAR(255)                NOT NULL,
    serialized_event       TEXT                        NOT NULL,
    publication_date       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP(6) WITH TIME ZONE,
    last_resubmission_date TIMESTAMP(6) WITH TIME ZONE,
    completion_attempts    INTEGER                     NOT NULL DEFAULT 0,
    status                 VARCHAR(255)                NOT NULL
        CHECK (status IN ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED')),

    CONSTRAINT pk_event_publication PRIMARY KEY (id)
);

CREATE INDEX idx_event_publication_status ON event_publication (status);
CREATE INDEX idx_event_publication_publication_date ON event_publication (publication_date);
```

### 3. `application-local.yml` / `application-dev.yml` 설정

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    virtual-host: msa
    username: ${RABBITMQ_USER:your-service}
    password: ${RABBITMQ_PASSWORD:your1234}
    publisher-confirm-type: correlated   # Outbox 발행 서비스 필수
    publisher-returns: true

  modulith:
    events:
      externalization:
        enabled: true
      republish-outstanding-events-on-restart: false  # 재발행은 messaging 모듈 스케줄러가 담당
      staleness:
        published: 1m
        processing: 5m
        resubmitted: 2m

bds:
  messaging:
    events:
      republish-delay: 30s
      republish-min-age: 1m
      retention: 7d
```

### 4. `RabbitTopologyConfig` 작성

커스텀 `RabbitTemplate`/`MessageConverter`를 선언하면 Spring Modulith의 AMQP externalization 경로를 방해하므로 반드시 **Exchange 선언과 `EventExternalizationConfiguration`만** 작성합니다.

```java
@Configuration
public class RabbitTopologyConfig {

    @Bean
    public TopicExchange myExchange() {
        return ExchangeBuilder.topicExchange("my.exchange").durable(true).build();
    }

    /**
     * Spring Modulith는 기본적으로 AutoConfigurationPackages(= 메인 클래스 패키지)만 스캔한다.
     * 이벤트가 libs/bds-events(com.bds.common.*)처럼 외부 패키지에 있을 경우
     * 패키지 필터에서 제외되어 externalization이 동작하지 않는다.
     * @Externalized 어노테이션 기준으로 전체 클래스패스를 대상으로 재정의한다.
     */
    @Bean
    public EventExternalizationConfiguration eventExternalizationConfiguration() {
        return EventExternalizationConfiguration.externalizing()
                .selectAndRoute(Externalized.class, Externalized::value)
                .build();
    }
}
```

> **주의:** `EventExternalizationConfiguration` 빈을 직접 정의하면 Spring Modulith의  
> auto-configured 버전(`@ConditionalOnMissingBean`)이 스킵됩니다.

### 5. 이벤트 정의 (`libs/bds-events`)

Direct 발행과 달리 `@Externalized`를 사용합니다.  
라우팅 목적지를 `"exchange::routingKey"` 형식으로 선언합니다.

```java
import org.springframework.modulith.events.Externalized;

@Externalized("my.exchange::my.event.occurred")
public record MyEvent(
        UUID eventId,
        Long targetId,
        Instant occurredAt
) {
    public static MyEvent of(Long targetId) {
        return new MyEvent(UUID.randomUUID(), targetId, Instant.now());
    }
}
```

### 6. 이벤트 발행

`ApplicationEventPublisher`를 주입받아 `@Transactional` 메서드 안에서 발행합니다.  
TX commit 이후 Spring Modulith가 자동으로 RabbitMQ로 externalize합니다.

```java
@Slf4j
@Component
public class MyEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public MyEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public void publishMyEvent(Long targetId) {
        log.info("publishMyEvent: targetId={}", targetId);
        applicationEventPublisher.publishEvent(MyEvent.of(targetId));
    }
}
```

**호출 위치:** `@Transactional` 메서드 내부에서 호출해야 합니다.

```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final MyEventPublisher myEventPublisher;

    @Transactional
    public void doSomething(Long targetId) {
        // 비즈니스 로직
        myEventPublisher.publishMyEvent(targetId);  // 같은 TX 안에서 발행
    }
}
```

> `publishMyEvent()` 자체가 `@Transactional`이므로 서비스 TX에 참여합니다.  
> TX가 commit될 때 `event_publication` → `COMPLETED`로 업데이트되고 AMQP 발행이 일어납니다.

### 7. 이벤트 수신 (Consumer)

Consumer 쪽은 Direct 발행과 동일합니다.

```java
@Configuration
public class MyQueues {
    public static final String MY_EVENT = "my-service.my-event.queue";

    @Bean
    public Declarables myEventQueue() {
        return BdsQueues.workQueue(MY_EVENT, "my.exchange", "my.event.occurred");
    }
}
```

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class MyEventListener {
    private final ProcessedEventStore processedEventStore;

    @Transactional
    @RabbitListener(queues = MyQueues.MY_EVENT)
    public void handle(MyEvent event) {
        if (!processedEventStore.markProcessed(event.eventId())) {
            log.info("중복 이벤트 스킵: {}", event.eventId());
            return;
        }
        log.info("MyEvent 수신: targetId={}", event.targetId());
        // 비즈니스 로직
    }
}
```

### 8. Outbox 상태 확인

`event_publication` 테이블로 발행 상태를 모니터링할 수 있습니다.

```bash
# 상태 확인
docker exec <postgres-container> psql -U postgres -d <db> \
  -c "SELECT status, event_type, publication_date, completion_date
      FROM event_publication ORDER BY publication_date DESC LIMIT 10;"
```

| status | 의미 |
|--------|------|
| `PUBLISHED` | TX commit 후 externalization 대기 또는 진행 중 |
| `COMPLETED` | AMQP 발행 성공 |
| `FAILED` | 발행 실패 — `BdsEventPublicationAutoConfig`의 스케줄러가 30s 간격으로 재시도 |
| `RESUBMITTED` | 재시도 중 |

---

## `ProcessedEventStore` 구현 (멱등성)

`ProcessedEventStore`는 동일 이벤트의 중복 처리를 막는 인터페이스입니다.  
구현 여부와 방식은 서비스의 비즈니스 요구사항에 따라 선택합니다.

```java
public interface ProcessedEventStore {
    // 처음 처리하는 eventId면 true 반환 (처리 진행)
    // 이미 처리한 eventId면 false 반환 (스킵)
    boolean markProcessed(UUID eventId);
}
```

### 구현체 선택 기준

| 상황 | 구현체 | 설명 |
|------|--------|------|
| 단일 인스턴스 / 재시작 시 중복 허용 | `InMemoryProcessedEventStore` | `ConcurrentHashMap` 사용, 가장 단순 |
| 다중 인스턴스 / 재시작 후도 중복 방지 | `RedisProcessedEventStore` (직접 구현) | Redis `SET NX` 활용 |
| 멱등성 검증 불필요 | 항상 `true` 반환하는 더미 구현체 | 비즈니스 로직 자체가 멱등일 때 |

### `InMemoryProcessedEventStore` 동작 원리

```java
@Component
public class InMemoryProcessedEventStore implements ProcessedEventStore {
    private final ConcurrentHashMap<UUID, Boolean> processed = new ConcurrentHashMap<>();

    @Override
    public boolean markProcessed(UUID eventId) {
        return processed.putIfAbsent(eventId, Boolean.TRUE) == null;
    }
}
```

- `ConcurrentHashMap.putIfAbsent()`는 atomic하게 동작하므로 멀티스레드 안전
- 단, 서버 재시작 시 맵이 초기화되므로 재시작 전후 중복 수신이 발생할 수 있음

---

## CI workflow 설정

`ci-your-service.yml`에 RabbitMQ 서비스 블록과 환경 변수를 추가합니다.

```yaml
jobs:
  build-and-test:
    runs-on: ubuntu-latest
    services:
      rabbitmq:
        image: rabbitmq:4
        env:
          RABBITMQ_DEFAULT_USER: your-service
          RABBITMQ_DEFAULT_PASS: your1234
          RABBITMQ_DEFAULT_VHOST: msa
        ports:
          - 5672:5672
        options: >-
          --health-cmd "rabbitmq-diagnostics -q ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 10
          --health-start-period 30s
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

      - name: Build and Test
        env:
          RABBITMQ_HOST: localhost
          RABBITMQ_PORT: 5672
          RABBITMQ_USER: your-service
          RABBITMQ_PASSWORD: your1234
        run: ./gradlew :services:your-service:clean :services:your-service:build
```

---

## 트러블슈팅

### Outbox 이벤트가 RabbitMQ에 전달되지 않음

**증상:** `event_publication`에 `PUBLISHED` 행은 생기지만 큐에 메시지가 도착하지 않음.

**원인 1: `EventExternalizationConfiguration` 패키지 스캔 범위 문제**

Spring Modulith는 기본적으로 `AutoConfigurationPackages`(= `@SpringBootApplication` 선언 클래스의 패키지)만 스캔합니다.  
이벤트가 `libs/bds-events`(com.bds.common.*)에 있으면 스캔 대상에서 제외됩니다.

→ `RabbitTopologyConfig`에 `EventExternalizationConfiguration` 빈을 직접 정의합니다 (4번 항목 참고).

**원인 2: `RabbitTemplate`을 직접 선언한 경우**

서비스 `@Configuration`에 커스텀 `RabbitTemplate` 빈이 있으면 Spring Modulith의 AMQP externalization이 사용하는 auto-configured 템플릿을 덮어씁니다.

→ `RabbitTemplate` / `MessageConverter` 빈 선언을 제거하고 `BdsRabbitAutoConfig`에 위임합니다.

**원인 3: `@Transactional` 외부에서 `publishEvent()` 호출**

Spring Modulith는 활성 TX 안에서 `publishEvent()`가 호출되어야 TX commit 후 externalization을 수행합니다.

→ `@Transactional` 메서드 안에서 발행하거나, 발행 메서드 자체에 `@Transactional`을 선언합니다.

---

## 모듈 구조 요약

```
libs/bds-events/
  └─ com.bds.common.events/
       ├─ PublishTo.java                        # Direct 발행용 목적지 선언 어노테이션
       └─ order/
            └─ OrderCreatedEvent.java           # @Externalized Outbox 이벤트 예시

modules/messaging/
  └─ com.bds.messaging/
       ├─ BdsRabbit.java                        # standardTemplate / standardListenerFactory 팩토리
       ├─ BdsRabbitAutoConfig.java              # MessageConverter + confirm/returns 콜백 자동 설정
       ├─ BdsQueues.java                        # workQueue (메인 큐 + DLQ 세트) 선언 헬퍼
       ├─ BdsEventPublicationAutoConfig.java    # 미완료 이벤트 재발행 스케줄러 (Outbox용)
       └─ idempotency/ProcessedEventStore.java  # 멱등성 인터페이스
```
