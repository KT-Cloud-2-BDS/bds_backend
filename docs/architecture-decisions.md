# 🏛 빵디즈 (BDS) — Architecture Decisions

> 프로젝트 진행 중 내린 주요 기술적 의사결정과 그 배경을 정리한 문서입니다.

---

## 1. 디렉토리 구조 (Monorepo)

여러 마이크로서비스를 하나의 저장소에서 관리하는 **Monorepo** 구조를 채택했습니다.

```
bds_backend/
├── .github/       // CI/CD 워크플로우, 이슈 템플릿
├── docs/          // API 스펙, 프로젝트 가이드
├── infra/         // RabbitMQ 로컬/dev 설정
├── libs/          // 서비스 간 공통 이벤트 타입
├── modules/       // 공통 유틸리티, 메시징 설정 (RabbitMQ 공통 설정)
├── platform/      // Config / Discovery 서버
└── services/
    ├── auth-service
    ├── chat-service
    ├── member-service
    └── ...기타 서비스
```

각 서비스 내부는 **Layered Architecture**를 따릅니다.

```
Presentation → Application → Domain → Infra
```

---

## 2. 서비스 간 통신 구조 (비동기)

이벤트 유실 없는 신뢰성 있는 비동기 통신을 위해 **Spring Modulith Outbox 패턴 + RabbitMQ**를 사용합니다.

**흐름 예시 (주문 생성 → 결제 서비스 알림)**

```
order-service --[@Externalized 이벤트 발행]--> order.exchange (Topic Exchange)
             --[routing key: order.created]--> payment.order-created.queue --> payment-service
```

- 이벤트는 `@Externalized` 어노테이션으로 발행되며, Spring Modulith Outbox → `event_publication` 테이블 → AMQP 순서로 외부 전달됩니다.
- **즉시 Publish**: 트랜잭션 커밋 시점에 바로 발행

### 핵심 설계 원칙

| 원칙 | 설명 |
|---|---|
| **Topic Exchange** | 서비스 간 메시지는 RabbitMQ Topic Exchange를 통해 비동기로 전달되거나, HTTP Request를 통해 동기적으로 전달됨 |
| **Outbox Pattern** | 유실이 허용되지 않는 이벤트는 Spring Modulith Outbox 패턴을 적용, 유실이 허용되는 이벤트는 Outbox DB 저장 없이 바로 publish |
| **Quorum Queue & DLQ** | 모든 큐는 Quorum Queue 기반이며, 재시도 5회 초과 시 DLQ(Dead Letter Queue)로 이동 |

---

## 3. 서비스 간 통신 방식 (동기 / 비동기 구분)

연결 목적에 따라 **비동기(Event-driven)**와 **동기(Request/Response)** 통신을 구분해서 사용합니다.

### 비동기 통신 (RabbitMQ)

| 도메인 관계 | 통신 수단 |
|---|---|
| 주문 ↔ 결제 | RabbitMQ(Broker)를 사용하여 비동기적으로 통신 |
| 펀딩 ↔ 채팅 | RabbitMQ(Broker)를 사용하여 비동기적으로 통신 |
| 채팅·주문·펀딩 → 알림 | RabbitMQ(Broker)를 사용하여 비동기적으로 통신 |

### 동기 통신 (Eureka / OpenFeign)

| 도메인 관계 | 통신 수단 |
|---|---|
| 인증 ↔ 채팅, Spring Cloud Gateway | Eureka를 통해 IP 주소를 찾아 HTTP Request를 사용하여 동기적으로 통신 |
| 인증 ↔ 멤버 | OpenFeign을 사용하여 동기적으로 통신 |

---

## 4. CI 구조 (Multi-module)

서비스별 파이프라인을 분리하여, **변경이 발생한 서비스만 선별적으로 빌드 및 테스트**를 실행합니다.

- **빌드 최적화 기법**: sparse checkout으로 해당 서비스와 의존 모듈만 체크아웃하여 시간 단축, Gradle 의존성 캐시를 활용해 매 빌드마다 재다운로드 방지
- **Matrix 전략**: 변경 감지(`detect-changes`) job을 통해 `git diff`로 변경된 서비스를 추출하고, 해당 서비스 목록을 기반으로 병렬 빌드 및 테스트 수행
- **Jacoco 커버리지**: auth, chat, member, payment 등 각 서비스 단위로 커버리지 리포트를 수집하여 품질 관리

---

## 5. CD 구조

서비스마다 워크플로우는 독립적이지만, 배포(CD) 로직은 `cd-common.yml` 하나의 재사용 가능한 워크플로우(`workflow_call`)로 통합되어 있습니다.

| 항목 | 설명 |
|---|---|
| **서비스별 독립 배포** | 서비스마다 워크플로우는 독립적이지만, 배포 로직은 단일화 |
| **빌드 아티팩트 재사용** | 이미지를 새로 빌드하기 위해 소스를 다시 받지 않고, 테스트를 통과시켜 업로드해둔 fatJAR을 그대로 내려받아 사용 → 이중 빌드 제거 |
| **이미지 태그 = 커밋 SHA** | Docker 이미지 태그를 커밋 SHA로 고정하여 배포 추적성 확보 — CI가 아니라 배포를 트리거한 run-id를 지목해 그대로 통합 |
| **SSM Run Command 기반 무중단 배포** | GitHub Actions에서 AWS SSM Run Command로 직접 배포, `--max-concurrency 1`, `--max-errors 0` 설정으로 순차 배포·실패 시 확산 중단 보장. 별도 배포 에이전트 설치 불필요 |
| **비밀 정보의 짧은 생존 주기** | DB 계정/API 키를 GitHub Secrets 대신 SSM Parameter Store(SecureString)에서 관리. 배포 스크립트가 대상 EC2 안에서 파라미터를 임시 env 파일로 받아 컨테이너에 주입한 뒤, 배포 직후 즉시 삭제 |
| **헬스체크 통과 후에만 배포 확정** | 컨테이너 교체 후 `/actuator/health`를 최대 150초간 폴링해 `status: UP` 확인한 뒤에야 배포 성공으로 처리. 실패 시 컨테이너 로그 100줄을 자동 덤프해 서버 접속 없이 원인 파악 가능 |
| **키 없는 인증 (OIDC)** | GitHub Actions는 AWS 액세스 키를 저장하지 않고, OIDC로 발급받는 임시 자격 증명을 사용. 신뢰 정책이 특정 레포·브랜치로 제한되어 다른 레포/브랜치는 배포 권한을 빌릴 수 없음 |

---

## 6. Terraform 인프라 (IaC)

전체 AWS 리소스(VPC, EC2, ALB, IAM, DNS 등)를 Terraform 코드로 선언하여 관리합니다.

| 항목 | 설명 |
|---|---|
| **인프라 On/Off 스위치** | `running` 변수 하나로 NAT Gateway, EC2, ALB까지 한 번에 stop/start. `terraform apply -var="running=false"` 한 줄로 리소스를 내리고, 다시 `true`로 원복. 비용을 최대 90% 이상 절감 |
| **최소 권한 3단계 IAM 분리** | 장기 액세스 키는 로컬 관리자 계정 하나로 한정. GitHub Actions(OIDC 임시 인증)와 EC2(인스턴스 프로파일)는 각각 필요한 권한만 가진 별도 역할로 분리. 하드코딩된 키가 어디에도 없음 |
| **리소스 간 자동 의존성 해석** | 서브넷 · 보안 그룹 · 역할을 이름으로 참조해, 하나가 재생성돼도 참조하는 리소스가 자동으로 최신 값을 따라감 — ID를 수동으로 복사해 옮기는 작업 불필요 |
| **네트워크 비용 최적화 (S3 Gateway Endpoint)** | 프라이빗 서브넷에서 S3(ECR 이미지 레이어 포함)로 가는 트래픽이 NAT Gateway를 우회하도록 VPC 엔드포인트를 구성. 배포마다 발생하는 이미지 pull 트래픽의 NAT 비용을 절감 |
| **HTTPS 인증서 자동 발급/갱신 (멀티 리전)** | ALB용 인증서(서울 리전)와 CloudFront용 인증서(us-east-1)를 각각의 provider로 자동 발급·검증. 인증서 만료·재발급을 수동으로 신경 쓸 필요 없음 |
| **서비스 목록과 파생 리소스의 단일 진실 소스** | ECR 리포지토리, 이미지 태그, on/off 대상이 서비스 목록(`instances` 변수) 하나로부터 자동 파생. 새 서비스 추가 시 목록 한 곳만 수정하면 관련 인프라가 이를 따라감 |
| **정적 프론트엔드 배포 인프라 (S3 + CloudFront + OAC)** | S3 버킷을 완전 비공개로 유지하고 CloudFront Origin Access Control(OAC)로만 접근 허용. 클라이언트 라우팅을 위한 403/404 → `index.html` 리다이렉트도 함께 구성 |

---

## 7. Event Broker 선택: RabbitMQ

### 도입 배경

빵디즈의 MSA 환경은 대규모 스트리밍보다 **간헐적인 이벤트 Publish**가 주를 이룹니다. 이런 트래픽 특성에서는 Kafka의 Pull 방식(지속적 poll, 복잡한 핸들링 필요)보다 RabbitMQ의 **Push 방식**이 인프라적 해결을 통한 개발 및 운영 난이도 감소 측면에서 더 적합하다고 판단했습니다.

### 선정 근거

| 근거 | 설명 |
|---|---|
| **리소스 효율성** | 메시지가 없을 때 TCP 연결만 유지 — Kafka 대비 가벼움 |
| **가벼운 구동** | 컨테이너 기동 시 약 200MB 메모리로 3초 만에 실행 (Kafka는 최소 1GB 이상 필요) |
| **자동 DLQ 처리** | 큐 선언 시 `x-dead-letter-exchange` 속성 한 줄로 실패 메시지가 자동으로 DLQ로 이동 (Kafka는 이런 처리를 직접 구현해야 함) |
| **장애 복구** | 브로커 다운 후 복구 시, 미처리 메시지 자동 환원 및 단순 재연결 지원 (Kafka의 Consumer Group Rebalancing 이슈 회피) |

---

## 참고

이 문서는 프로젝트 발표자료(중간/최종 발표 슬라이드) 중 아키텍처·인프라 관련 의사결정 부분을 발췌·정리한 것입니다. 발표자료 원본은 [프레젠테이션 자료](../docs/presentations/) 참고.
