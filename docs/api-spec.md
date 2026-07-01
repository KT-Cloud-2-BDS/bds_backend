# API 명세서 — BDS

> **Base URL** : `https://api.bds.com` (로컬: `http://localhost:8080`)  
> **인증** : `Authorization: Bearer {accessToken}` 헤더 필요 (Auth Required = O인 경우)

---

## 서비스별 API Docs & Repository
MSA 구조 변경에 따라 각 마이크로 서비스의 상세 API 문서 및 소스 코드는 아래 링크를 참조해 주세요.

*로컬 환경은 개별 서비스를 실행한 후 해당 포트로 접속이 가능합니다.*

| 마이크로서비스 | 로컬 포트 | 라우팅 경로 (Path) | API 명세서                                 | GitHub Repository                                                                 |
| :--- | :---: | :---: |:---------------------------------------------|:----------------------------------------------------------------------------------|
| **Auth Service** (인증/회원) | `8081` | `/auth` | [api-spec-auth](https://github.com/KT-Cloud-2-BDS/bds_backend/blob/main/services/auth-service/docs/api-spec-auth.md)         | [bds-auth-service](https://github.com/KT-Cloud-2-BDS/bds_backend/tree/main/services/auth-service)                  |
| **Chat Service** (채팅) | `8082` | `/chat` | [api-spec-chat](https://github.com/KT-Cloud-2-BDS/bds_backend/blob/main/services/chat-service/docs/api-spec-chat.md)         | [bds-chat-service](https://github.com/KT-Cloud-2-BDS/bds_backend/tree/main/services/chat-service)                  |
| **Notification Service** (알림) | `8083` | `/notification` | [api-spec-notification](https://github.com/KT-Cloud-2-BDS/bds_backend/blob/main/services/notification-service/docs/api-spec-notification.md) | [bds-notification-service](https://github.com/KT-Cloud-2-BDS/bds_backend/tree/main/services/notification-service)  |
| **Order Service** (주문) | `8084` | `/order` | [api-spec-order](https://github.com/KT-Cloud-2-BDS/bds_backend/blob/main/services/order-service/docs/api-spec-order.md)        | [bds-order-service](https://github.com/KT-Cloud-2-BDS/bds_backend/tree/main/services/order-service)              |
| **Payment Service** (결제) | `8085` | `/payment` | [api-spec-payment](https://github.com/KT-Cloud-2-BDS/bds_backend/blob/main/services/payment-service/docs/api-spec-payment.md)  |  [bds-payment-service](https://github.com/KT-Cloud-2-BDS/bds_backend/tree/main/services/payment-service)  |
---

## 공통 응답 포맷

### 성공 응답

도메인 DTO를 HTTP 상태 코드와 함께 직접 반환합니다.

#### 200 OK - 단일 객체 예시
```json
{
    "memberId": 1,
    "nickname": "응원왕",
    "email": "user@example.com"
}
```

#### 200 OK — 배열 예시
```json

[
    {
        "teamId": 1,
        "name": "KIA 타이거즈"
    },
    {
        "teamId": 2,
        "name": "FC 서울"
    }
]
```

### 에러 응답

```json
{
  "code": "SEAT_ALREADY_RESERVED",
  "message": "이미 선점된 좌석입니다.",
  "detail": null
}
```

### HTTP 상태 코드

| 코드  | 설명                             |
|-----|--------------------------------|
| 101 | HTTP 101 Switching Protocal 성공 |
| 200 | 성공                             |
| 201 | 리소스 생성 성공                      |
| 204 | 성공 (응답 본문 없음)                  |
| 400 | 요청 파라미터/바디 오류                  |
| 401 | 인증 실패 (토큰 없음 또는 만료)            |
| 403 | 권한 없음                          |
| 404 | 리소스 없음                         |
| 409 | 충돌 (중복, 이미 존재)                 |
| 410 | 리소스 만료 (예: 주문 만료)              |
| 422 | 비즈니스 규칙 위반                     |
| 429 | 요청 한도 초과                       |
| 500 | 서버 내부 오류                       |

### 공통 에러 코드

| 에러 코드                     | HTTP | 설명                          |
|---------------------------|------|-----------------------------|
| `INVALID_INPUT`           | 400  | 입력값 유효성 오류                  |
| `REQUEST_INVALID`         | 400  | Request Body 누락             |
| `REQUEST_BODY_MALFORMED`  | 400  | JSON 형식 오류                  |
| `UNAUTHORIZED`            | 401  | 인증 필요                       |
| `TOKEN_EXPIRED`           | 401  | Access Token 만료             |
| `TOKEN_INVLIAD`           | 401  | Access Token 유효성 검증 실패      |
| `FORBIDDEN`               | 403  | 접근 권한 없음                    |
| `NOT_FOUND`               | 404  | 리소스 없음                      |
| `METHOD_NOT_ALLOWED`      | 405  | 지원하지 않는 HTTP 메소드            |
| `CONFLICT`                | 409  | 리소스 충돌 (중복)                 |
| `BUSINESS_RULE_VIOLATION` | 422  | 비즈니스 규칙 위반                  |
| `RESOURCE_LOCKED`         | 423  | 리소스가 잠겨있음                   |
| `TOO_MANY_REQUESTS`       | 429  | 요청 한도 초과                    |
| `INTERNAL_ERROR`          | 500  | 서버 내부 오류                    |
| `SERVICE_UNAVAILABLE`     | 503  | 서비스 일시적 사용 불가               |
| `DEPENDENCY_FAILURE`      | 503  | 외부 서비스(Auth, Redis 등) 호출 실패 |

---

