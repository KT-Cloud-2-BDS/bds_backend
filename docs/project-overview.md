# 📦 빵디즈 (BDS) - 프로젝트 개요

> **"펀딩에 소통을 더하다, 우리들의 펀딩 놀이터 빵디즈"**
> [cite_start]본 프로젝트는 MSA 분산 환경에서 저지연 응답과 데이터 무결성을 보장하는 커뮤니티형 크라우드 펀딩 플랫폼입니다[cite: 3, 5].

---

## 1. 프로젝트 정보

* [cite_start]**개발 기간**: 2026.06.25 ~ 2026.07.28 (약 5주) [cite: 1]
* **개발 팀**: 빵빠레 (Backend)
* [cite_start]**목적**: 학습 및 핵심 기술 구현 목적 (Business Goals 제외) [cite: 5]

---

## 2. 배경 및 문제 정의

* [cite_start]**기존 플랫폼의 한계**: 기존 펀딩 플랫폼(예: 와디즈)은 후원자-창작자, 후원자-후원자 간의 직접적인 소통 수단이 부족합니다[cite: 1]. [cite_start]이로 인해 후원자는 펀딩 달성률 외에 제품의 인기나 실질적 필요성을 가늠할 근거가 부족하여 충동 후원 및 실패로 이어지기 쉽습니다[cite: 2, 4].
* [cite_start]**빵디즈의 해결책**: 프로젝트별 **실시간 공개 채팅방**을 제공하여 제품에 대한 투의를 가능하게 합니다[cite: 3]. [cite_start]이를 통해 후원자가 자신에게 정말 필요한 상품인지 판단할 기준을 세우도록 돕고, 실패하는 후원 경험을 최소화합니다[cite: 3, 4].

---

## 3. 핵심 도메인 규칙

* **펀딩 방식**: **All-or-Nothing** (마감 시점 기준 목표 금액 미달 시 전액 자동 환불)
* **결제 방식**: 후원 시 개인 월렛 잔액에서 **즉시 결제** 진행
* **펀딩 상태 라이프사이클**: `예정` ➡️ `진행중` ➡️ `성공 / 실패` ➡️ `종료`
* **재고 관리**: 수량 제한이 있는 리워드 기반 상품 구조

---

## 4. 핵심 기능 요구사항 (Functional Requirements)

[cite_start]각 핵심 기능은 상호 의존성을 최소화하기 위해 **독립적인 도메인**으로 분리하여 설계되었습니다[cite: 11].

### 📦 주문 (Order)
* [cite_start]**기능 요약**: 리워드 기반 커머스의 핵심 결제 파이프라인 및 재고 정합성 보장 [cite: 12, 13]
* **핵심 명세**:
    * [cite_start]단일 상품 내 여러 리워드 동시 선택 가능 (타 상품과 복합 주문 불가) [cite: 14, 15]
    * [cite_start]결제 진입 시 즉시 재고가 차감되며, 15분 이내 미결제 시 주문 취소 및 재고 복원 [cite: 17, 18]
* [cite_start]**성공 지표**: 동시 100건 주문 시 데이터 정합성 100% 유지, 주문 생성 API $p95 \le 200ms$ [cite: 19]

### 🔔 알림 (Notification)
* [cite_start]**기능 요약**: 사용자 맞춤형 펀딩 생명주기 및 채팅 알림 제공 [cite: 19, 20]
* **핵심 명세**:
    * [cite_start]**SSE(Server-Sent Events)** 방식을 기본으로 토픽 구독 처리 [cite: 22]
    * [cite_start]미접속 등 SSE Emitter가 없는 유저에게는 **FCM(Firebase Cloud Messaging)**으로 대체 발송 [cite: 23, 29]
    * [cite_start]펀딩 시작/성공/실패 알림 및 광고성 프로모션 동의자 대상 알림 발송 [cite: 23, 24, 25, 26]
* [cite_start]**성공 지표**: 새로운 이벤트 발생 시 1000ms 이내에 유저에게 알림 전달 [cite: 27]

### 💳 결제 및 월렛 (Payment & Pay)
* [cite_start]**기능 요약**: 조건부 정산(All-or-Nothing) 구조를 안전하게 처리하기 위한 가상 금융 인프라 [cite: 30, 32]
* **핵심 명세**:
    * [cite_start]**개인 월렛**: 가상 은행 계좌 조회 및 1원 인증 기반 등록, Webhook 기반 충전 [cite: 33, 34, 35]
    * [cite_start]**동시성 제어**: 비관적 락(Pessimistic Lock) 및 `tranSeqNo` 중복 검증을 통한 이중 충전/출금 방지 [cite: 36, 37, 38]
    * [cite_start]**보상 트랜잭션**: 타임아웃 등 외부 금융망 장애 시 차감된 잔액을 복원하는 Saga 패턴 적용 [cite: 37, 41]
    * [cite_start]**배치 정산/환불**: 펀딩 성공 시 창작자 이체, 실패 시 전원 자동 환불 진행 [cite: 38]
* [cite_start]**성공 지표**: 데이터 정합성 및 동시성 제어 성공률 100%, 보상 트랜잭션 누락 0건 [cite: 43]

### 💬 실시간 채팅 (Chat)
* [cite_start]**기능 요약**: 창작자와 후원자 간 신뢰 형성을 위한 양방향 실시간 소통 [cite: 53, 54]
* **핵심 명세**:
    * [cite_start]**WebSocket + STOMP** 기반 연결 및 JWT 핸드셰이크 인증 적용 [cite: 56]
    * [cite_start]**외부 브로커(RabbitMQ)** 중계를 통해 다중 인스턴스 환경에서도 메시지 일관성(Fan-out) 보장 [cite: 57, 58]
    * [cite_start]텍스트 메시지만 허용하며 글자 수는 300자로 제한, 최근 메시지 영속화 [cite: 55, 59]
* [cite_start]**성공 지표**: 인스턴스 간 메시지 전달 성공률 100%, 목표 동시 접속자 수에서 에러율 < 1% [cite: 59, 60]

### 🔑 회원 (Member)
* [cite_start]**기능 요약**: 플랫폼 보안과 원활한 활동을 위한 인증/인가 및 권한 관리 [cite: 44]
* **핵심 명세**:
    * [cite_start]이메일 인증(SMTP) 회원가입 및 OAuth 2.0 소셜 로그인 계정 연동 (`LOCAL`, `KAKAO`, `GOOGLE`) [cite: 45, 46, 47]
    * [cite_start]BCrypt 비밀번호 암호화 및 JWT (Access / Refresh Token) 기반 로그인 [cite: 45, 47]
    * 로그인 실패 제한(5회 초과 시 10분 제한) 및 Redis 활용 로그아웃 블랙리스트 처리
* [cite_start]**성공 지표**: 이메일 인증 메일 도달 3s 이내, 로그인 토큰 검증 지연 50ms 이내 [cite: 50, 51]

---

## 5. 비기능 요구사항 (Non-Functional Requirements)

* **성능 (Performance)**: 주문 폭증 시에도 대기열 제어 등을 통해 전체 시스템 응답 시간 $P95 < 3000ms$ 유지 (부하 테스트 후 수치 보정 예정)
* [cite_start]**보안 (Security)**: 상품 목록 및 상세 페이지는 미인증 사용자에게도 공개하되, 주문/결제/채팅 참여는 반드시 JWT 인증 통과 필요 [cite: 61, 62]

---

## 6. 기술 스택 (Tech Stack)


| 분류 | 기술 이름 | 비고 |
| :--- | :--- | :--- |
| **Backend** | *To be filled* | 예: Java 25, Spring Boot 4.0.3 |
| **Database** | *To be filled* | 예: PostgreSQL 18, Redis 8 |
| **Message Broker** | *To be filled* | 예: RabbitMQ |
| **Infra & DevOps** | *To be filled* | 예: Docker, GitHub Actions |
| **Monitoring** | *To be filled* | 예: Prometheus, Grafana, JMeter |

---

## 7.시스템 아키텍처
To be filled
---

## 8. 비용 및 인프라 설계

### 인프로 자우너 구성 안
To be filled

### 월 예상 비용 산정
To be filled

