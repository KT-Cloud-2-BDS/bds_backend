# 📦 빵디즈 (BDS) - 프로젝트 개요

> **"펀딩에 소통을 더하다, 우리들의 펀딩 놀이터 빵디즈"**
> 본 프로젝트는 MSA 분산 환경에서 저지연 응답과 데이터 무결성을 보장하는 커뮤니티형 크라우드 펀딩 플랫폼입니다.

---

## 1. 프로젝트 정보

* **개발 기간**: 2026.06.25 ~ 2026.07.28 (약 5주)
* **개발 팀**: 빵빠레 (Backend)
* **목적**: 학습 및 핵심 기술 구현 목적 (Business Goals 제외)

---

## 2. 배경 및 문제 정의

* **기존 플랫폼의 한계**: 기존 펀딩 플랫폼(예: 와디즈)은 후원자-창작자, 후원자-후원자 간의 직접적인 소통 수단이 부족합니다. 이로 인해 후원자는 펀딩 달성률 외에 제품의 인기나 실질적 필요성을 가늠할 근거가 부족하여 충동 후원 및 실패로 이어지기 쉽습니다.
* **빵디즈의 해결책**: 프로젝트별 **실시간 공개 채팅방**을 제공하여 제품에 대한 토의를 가능하게 합니다. 이를 통해 후원자가 자신에게 정말 필요한 상품인지 판단할 기준을 세우도록 돕고, 실패하는 후원 경험을 최소화합니다.

---

## 3. 핵심 도메인 규칙

* **펀딩 방식**: **All-or-Nothing** (마감 시점 기준 목표 금액 미달 시 전액 자동 환불)
* **결제 방식**: 후원 시 개인 월렛 잔액에서 **즉시 결제** 진행
* **펀딩 상태 라이프사이클**: `예정` ➡️ `진행중` ➡️ `성공 / 실패` ➡️ `종료`
* **재고 관리**: 수량 제한이 있는 리워드 기반 상품 구조

---

## 4. 핵심 기능 요구사항 (Functional Requirements)

각 핵심 기능은 상호 의존성을 최소화하기 위해 **독립적인 도메인**으로 분리하여 설계되었습니다.

### 📦 주문 (Order)
* **기능 요약**: 리워드 기반 커머스의 핵심 결제 파이프라인 및 재고 정합성 보장
* **핵심 명세**:
    * 단일 상품 내 여러 리워드 동시 선택 가능 (타 상품과 복합 주문 불가)
    * 결제 진입 시 즉시 재고가 차감되며, 15분 이내 미결제 시 주문 취소 및 재고 복원
* **성공 지표**: 동시 100건 주문 시 데이터 정합성 100% 유지, 주문 생성 API $p95 \le 200ms$

### 🔔 알림 (Notification)
* **기능 요약**: 사용자 맞춤형 펀딩 생명주기 및 채팅 알림 제공
* **핵심 명세**:
    * **SSE(Server-Sent Events)** 방식을 기본으로 토픽 구독 처리
    * 미접속 등 SSE Emitter가 없는 유저에게는 **FCM(Firebase Cloud Messaging)**으로 대체 발송
    * 펀딩 시작/성공/실패 알림 및 광고성 프로모션 동의자 대상 알림 발송
* **성공 지표**: 새로운 이벤트 발생 시 1000ms 이내에 유저에게 알림 전달

### 💳 결제 및 월렛 (Payment & Pay)
* **기능 요약**: 조건부 정산(All-or-Nothing) 구조를 안전하게 처리하기 위한 가상 금융 인프라
* **핵심 명세**:
    * **개인 월렛**: 가상 은행 계좌 조회 및 1원 인증 기반 등록, Webhook 기반 충전
    * **동시성 제어**: 비관적 락(Pessimistic Lock) 및 `tranSeqNo` 중복 검증을 통한 이중 충전/출금 방지
    * **보상 트랜잭션**: 타임아웃 등 외부 금융망 장애 시 차감된 잔액을 복원하는 Saga 패턴 적용
    * **배치 정산/환불**: 펀딩 성공 시 창작자 이체, 실패 시 전원 자동 환불 진행
* **성공 지표**: 데이터 정합성 및 동시성 제어 성공률 100%, 보상 트랜잭션 누락 0건

### 💬 실시간 채팅 (Chat)
* **기능 요약**: 창작자와 후원자 간 신뢰 형성을 위한 양방향 실시간 소통
* **핵심 명세**:
    * **WebSocket + STOMP** 기반 연결 및 JWT 핸드셰이크 인증 적용
    * **외부 브로커(RabbitMQ)** 중계를 통해 다중 인스턴스 환경에서도 메시지 일관성(Fan-out) 보장
    * 텍스트 메시지만 허용하며 글자 수는 300자로 제한, 최근 메시지 영속화
* **성공 지표**: 인스턴스 간 메시지 전달 성공률 100%, 목표 동시 접속자 수에서 에러율 < 1%

### 🔑 회원 (Member)
* **기능 요약**: 플랫폼 보안과 원활한 활동을 위한 인증/인가 및 권한 관리
* **핵심 명세**:
    * 이메일 인증(SMTP) 회원가입 및 OAuth 2.0 소셜 로그인 계정 연동 (`LOCAL`, `KAKAO`, `GOOGLE`)
    * BCrypt 비밀번호 암호화 및 JWT (Access / Refresh Token) 기반 로그인
    * 로그인 실패 제한(5회 초과 시 10분 제한) 및 Redis 활용 로그아웃 블랙리스트 처리
* **성공 지표**: 이메일 인증 메일 도달 3s 이내, 로그인 토큰 검증 지연 50ms 이내

---

## 5. 비기능 요구사항 (Non-Functional Requirements)

* **성능 (Performance)**: 주문 폭증 시에도 대기열 제어 등을 통해 전체 시스템 응답 시간 $P95 < 3000ms$ 유지 (부하 테스트 후 수치 보정 예정)
* **보안 (Security)**: 상품 목록 및 상세 페이지는 미인증 사용자에게도 공개하되, 주문/결제/채팅 참여는 반드시 JWT 인증 통과 필요

---

## 6. 기술 스택 (Tech Stack)

| 분류 | 기술 이름 | 비고 |
| :--- | :--- | :--- |
| **Backend** | Java 25, Spring Boot 4.1.0, Spring Cloud 2025.1.2 (Oakwood) | MSA 기반 서비스별 독립 배포 |
| **인증 & 실시간** | Spring Security, JWT, WebSocket, STOMP | 채팅 핸드셰이크 시 JWT 자체 검증 |
| **서비스 통신** | Spring Cloud Gateway, Eureka (Service Discovery), RabbitMQ:4, OpenFeign | 동기(REST/Feign) + 비동기(MQ) 이벤트 혼용 |
| **Database** | PostgreSQL 17 (Auth/Member/Chat/Payment/Notification), MySQL 8.4 (Order), Redis | 서비스별 DB 분리 (Database per Service), Redis는 인증/알림 캐시 및 토큰 블랙리스트용 |
| **ORM** | Spring Data JPA | |
| **Test** | JUnit 5, Mockito, Testcontainers | |
| **Infra & DevOps** | Docker, Docker Compose, AWS (EC2, ALB, NAT Gateway, S3, CloudFront, Route 53, ECR), GitHub Actions, CodeDeploy / ASG | Monorepo 기반 CI/CD, 변경된 서비스만 Build & Deploy |
| **Monitoring** | Prometheus, Grafana, CloudWatch, SSM Parameter Store | 서비스별 컨테이너 동거형 메트릭 수집 |

---

## 7. 시스템 아키텍처

![빵디즈 시스템 아키텍처](./system-architecture.png)

### 7.1 트래픽 흐름 (Public)
* **사용자 → Route 53 → CloudFront (CDN) / ALB**
    * 정적 프론트(SPA)는 **S3 + CloudFront**로 서빙 (`front.도메인`), 프론트 빌드 배포는 S3 sync + CloudFront invalidation 방식
    * API 요청(`api.도메인`)은 **Internet Gateway → ALB(Ingress)**를 거쳐 Public Subnet 진입
    * WebSocket(`/ws`)은 핸드셰이크 시에만 Gateway를 거치고, 이후에는 ALB에서 채팅 서비스로 직행 라우팅되어 Gateway 부하를 줄임 (핸드셰이크 이후 채팅 서비스가 JWT를 자체 검증)

### 7.2 Private Subnet — EC2 Application Server
* **Gateway EC2**: Spring Cloud Gateway가 Discovery EC2(Eureka)에서 라우팅 대상을 조회하여 각 서비스로 요청을 분기
* **Discovery EC2**: Eureka Server가 컨테이너로 동거하며 전 서비스의 등록(heartbeat) 및 주소 조회를 처리
* **서비스별 Docker 컨테이너** (Auth / Member / Chat / Payment / Order / Notification): 각 서비스는 독립된 EC2(컨테이너)와 전용 DB를 가지며, 인증 서비스는 JWKS(서명 공개키)를 발급해 타 서비스 및 WS 핸드셰이크 검증에 활용됨
* **RabbitMQ on EC2**: 단일 브로커로 이벤트 발행/구독을 중계 (채팅 Fan-out, 알림 이벤트 등 비동기 처리)
* **데이터 계층**: 서비스별 PostgreSQL/MySQL 컨테이너 + Redis(인증, 알림용) — Database per Service 원칙 준수

### 7.3 외부 연동 & 운영
* **외부 서비스**: OAuth Provider(Google, Kakao), FCM 푸시, SMTP 메일 발송 — NAT Gateway를 통한 아웃바운드로 연동
* **모니터링**: 각 서비스 EC2에 Prometheus 익스포터 컨테이너가 동거하며 자체 수집 → Grafana 대시보드로 시각화, CloudWatch는 인프라 로그·알람 담당
* **설정 관리**: SSM Parameter Store에서 기동 시 설정/시크릿을 로드

### 7.4 CI/CD — Monorepo
```
GitHub Monorepo (feature → PR develop/main)
   → PR 생성 시 CI 트리거 (GitHub Actions)
   → 변경 경로 감지(path filter)로 변경된 서비스만 Build & Test
   → merge 시 이미지 빌드 후 Amazon ECR에 푸시
   → CD(GitHub Actions)가 EC2 Rolling Deploy 수행 (CodeDeploy / ASG)
   → 프론트엔드는 별도로 S3 sync + CloudFront invalidation
```

---

## 8. 비용 및 인프라 설계

### 8.1 인프라 자원 구성안

| 자원 | 구성 | 수량 |
| :--- | :--- | :---: |
| EC2 (t3.medium) | 서비스 컨테이너 호스팅 — Auth, Member, Chat, Payment, Order, Notification | 6 |
| EC2 (t3.small) | 인프라 컴포넌트 — Gateway, Discovery(Eureka), RabbitMQ | 3 |
| EC2 (t3.small, Redis) | Redis (인증 토큰/알림 캐시) | 1 |
| NAT Gateway | Private Subnet 아웃바운드 (OAuth, FCM, SMTP 등) | 1 |
| ALB | Ingress, 고정 요금 + LCU 종량 | 1 |
| 퍼블릭 IPv4 | NAT Gateway + ALB용 | 1 |
| EBS | 인스턴스당 30GB × 10대 = 300GB | 300GB |
| S3 / CloudFront / Route 53 | 정적 프론트(SPA) 배포 및 DNS | - |
| 기타 | 데이터 전송, ECR, CloudWatch 등 | 실측 반영 |

### 8.2 월 예상 비용 산정

**구성요소별 시간당 단가**

| 구성 요소 | 수량 | 단가 ($/시간) | 소계 ($/시간) |
| :--- | :---: | :---: | :---: |
| EC2 t3.medium (Auth·Member·Chat·Payment·Order·Notification) | 6 | $0.052 | $0.312 |
| EC2 t3.small (Gateway·Discovery·RabbitMQ) | 3 | $0.026 | $0.078 |
| Redis EC2 (t3.small 가정) | 1 | $0.026 | $0.026 |
| NAT Gateway | 1 | $0.059 | $0.059 |
| ALB (고정 + LCU) | 1 | $0.0225 + $0.008 | $0.031 |
| 퍼블릭 IPv4 (NAT + ALB) | 1 | $0.005 | $0.015 |
| EBS 볼륨 (10대 × 30GB) | 300GB | $0.0912/GB·월 (환산) | $0.037 |
| 데이터 전송 · ECR · CloudWatch 등 (실측 역산) | - | - | $0.07 |
| **실측 기준 합계** | | | **$0.628 / 시간** |

**월간 예산 시나리오**

| 시나리오 | 24시간 상시 가동 | 평일 09~18시 · 22일 영업 | 완전 OFF |
| :--- | :---: | :---: | :---: |
| **월 예상 비용** | **$163.8** (약 226,000원) | **$58.6** (약 81,000원) | **$24.5** |
| 산정 근거 | $5.46/일 × 30일 = $163.8 | 176시간 × $0.228 = $40.1 (가동 시간) <br> + 544시간 × $0.034 = $18.5 (비가동 시간, 고정비) <br> = 합계 $58.6 | EBS + 호스팅존 등 고정 비용만 발생 |

> 환율 1,380원 기준 환산. 실제 트래픽/부하 테스트 결과에 따라 인스턴스 스펙 및 비용은 조정될 수 있음.

---
