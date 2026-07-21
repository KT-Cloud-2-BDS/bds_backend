## payment 도메인

### payment 도메인 엔드포인트 목록

| method | path                                    | auth required | 설명                              |
|--------|------------------------------------------|---------------|---------------------------------|
| GET    | /api/payment/wallet                     | O             | 월렛 잔액 조회                        |
| POST   | /api/payment/accounts                   | O             | 계좌 등록                           |
| POST   | /api/payment/accounts/verify            | O             | 1원 인증                           |
| POST   | /api/payment/deposit                    | O             | 페이 충전                           |
| POST   | /api/payment/withdraw                   | O             | 페이 출금                           |
| GET    | /api/payment/history                    | O             | 개인 월렛 거래 내역 조회                  |
| POST   | /api/payment/funding                    | X             | 펀딩 결제 (`RabbitMQ`로 구현 예정)       |
| POST   | /api/payment/funding/settlement         | X             | 펀딩 정산 요청 (`RabbitMQ`로 구현 예정)    |
| POST   | /api/payment/funding/batch-refund       | X             | 펀딩 배치 환불 요청 (`RabbitMQ`로 구현 예정) |
| POST   | /api/payment/refund                     | O             | 수동 환불 (`RabbitMQ`로 구현 예정)       |

### 가상 금융망 엔드포인트 목록

| method | path                                   | auth required | 설명                    |
|--------|------------------------------------------|---------------|------------------------|
| POST   | /api/banks/accounts                    | X             | 계좌 실명 조회 + 1원 송금 |
| POST   | /api/banks/accounts/verify             | X             | 인증 코드 조회           |
| POST   | /api/banks/withdraw                    | X             | 계좌 출금 (페이 충전)    |
| POST   | /api/banks/deposit                     | X             | 계좌 입금 (원화 환불)    |
| GET    | /api/banks/transactions/{tranSeqNo}    | X             | 거래 코드 조회           |

---

### 월렛 잔액 조회

```
GET /api/payment/wallet
```

Auth Required: **O**

Request Body: 없음 (`@LoginUser`로 주입된 `CurrentUser`로 사용자 식별)

Response Body

```json
{
  "memberId": 1,
  "balance": 50000
}
```

Validation / Business Rules

- 인증된 사용자 본인의 월렛 잔액만 조회 가능.
- 인증 정보는 `@LoginUser` 어노테이션을 통해 컨트롤러 파라미터로 `CurrentUser` 객체로 주입됩니다.

---

### 계좌 등록

```
POST /api/payment/accounts
```

Auth Required: **O**

Request Body (`@LoginUser`로 주입된 `CurrentUser`로 사용자 식별)

| 필드              | 타입     | 필수 | 설명      |
|------------------|--------|-----|---------|
| `bankCode`       | String | Y   | 은행 코드   |
| `accountNumber`  | String | Y   | 계좌 번호   |
| `holderName`     | String | Y   | 예금주 이름  |

Response Body

```json
{
  "message" : "정상 처리되었습니다."
}
```

Validation / Business Rules

- 인증된 사용자만 등록 가능.
- 이미 등록된 계좌일 경우 중복 등록 방지.
- 클라이언트가 입력한 `holderName`과 가상 은행 실명 조회 결과 일치 여부 검증.
- 불일치 시 등록 실패 처리.
- 계좌 등록 후 1원 인증이 완료되어야 최종 등록 완료.
- 다른 계좌를 등록하려면 기존 계좌를 삭제하고 재등록 진행.

---

### 1원 인증

```
POST /api/payment/accounts/verify
```

Auth Required: **O**

Request Body (`@LoginUser`로 주입된 `CurrentUser`로 사용자 식별)

| 필드     | 타입     | 필수 | 설명        |
|--------|--------|-----|-----------|
| `code` | String | Y   | 1원 인증 코드  |

Response Body

```json
{
  "message": "정상 처리되었습니다."
}
```

Validation / Business Rules

- 인증된 사용자만 요청 가능.
- 가상 은행에 인증 코드 조회 후 일치 시 계좌 등록 완료.
- 코드 불일치 시 인증 실패 처리.
- 인증 정보는 `@LoginUser` 어노테이션을 통해 컨트롤러 파라미터로 `CurrentUser` 객체로 주입됩니다.

---

### 페이 충전

```
POST /api/payment/deposit
```

Auth Required: **O**

Request Body (`@LoginUser`로 주입된 `CurrentUser`로 사용자 식별)

| 필드       | 타입   | 필수 | 설명     |
|----------|------|-----|--------|
| `amount` | Long | Y   | 충전 금액  |

Response Body

```json
{
  "balance": 60000,
  "tranSeqNo": "018e1234-abcd-7xxx-xxxx-xxxxxxxxxxxx"
}
```

Validation / Business Rules

- 인증된 사용자만 요청 가능.
- 등록된 계좌가 없을 경우 충전 불가.
- Pay 서버가 가상 은행 계좌에서 출금 요청 후 성공 시 월렛 잔액 증가.
- `tranSeqNo` 중복 검증으로 이중 충전 방지.
- `tranSeqNo`는 UUID v7 사용.
- 처리 완료 시 `payment_history`에 `type: DEPOSIT`으로 기록.
- 인증 정보는 `@LoginUser` 어노테이션을 통해 컨트롤러 파라미터로 `CurrentUser` 객체로 주입됩니다.

---

### 페이 출금

```
POST /api/payment/withdraw
```

Auth Required: **O**

Request Body (`@LoginUser`로 주입된 `CurrentUser`로 사용자 식별)

| 필드       | 타입   | 필수 | 설명     |
|----------|------|-----|--------|
| `amount` | Long | Y   | 출금 금액  |

Response Body

```json
{
  "balance": 40000,
  "tranSeqNo": "018e1234-abcd-7xxx-xxxx-xxxxxxxxxxxx"
}
```

Validation / Business Rules

- 인증된 사용자만 요청 가능.
- 등록된 계좌가 없을 경우 출금 불가.
- 월렛 잔액 먼저 차감 후 가상 은행에 입금 요청.
- 가상 은행 타임아웃 시 차감한 잔액 복원 (보상 트랜잭션).
- 타임아웃 발생 시 거래 코드 조회로 처리 여부 확인.
  - 존재할 경우 정상 처리.
  - 존재하지 않을 경우 잔액 복원.
- `tranSeqNo` 중복 검증으로 이중 출금 방지.
- `tranSeqNo`는 UUID v7 사용.
- 처리 완료 시 `payment_history`에 `TransactionType.WITHDRAW` 및 `TransactionReason.WITHDRAW`로 기록됩니다.
- 인증 정보는 `@LoginUser` 어노테이션을 통해 컨트롤러 파라미터로 `CurrentUser` 객체로 주입됩니다.

---

### 상품 가상계좌 생성
 
```
POST /api/payment/products
```

Auth Required: **O**

Request Body: 없음 (`@LoginUser`로 주입된 `CurrentUser`로 사용자 식별)

Query Parameter

| 필드        | 타입     | 필수 | 설명                    |
|-----------|--------|-----|-----------------------|
| `from`    | String | N   | 조회 시작일 (ISO-8601, 예: 2026-05-01). 미지정 시 현재로부터 1개월 전 |
| `to`      | String | N   | 조회 종료일 (ISO-8601). 미지정 시 현재 시각 |
| `page`    | Int    | N   | 페이지 번호 (기본값 0)      |
| `size`    | Int    | N   | 페이지당 개수 (기본값 20)    |

Response Body
 
```json
{
  "content": [
    {
      "tranSeqNo": "018e1234-abcd-7xxx-xxxx-xxxxxxxxxxxx",
      "type": "CHARGE",
      "message": "페이 충전",
      "amount": 10000,
      "balanceAfter": 60000,
      "status": "SUCCESS",
      "createdAt": "2026-06-01T10:00:00"
    },
    {
      "tranSeqNo": "018e5678-abcd-7xxx-xxxx-xxxxxxxxxxxx",
      "type": "WITHDRAW",
      "message": null,
      "amount": 10000,
      "balanceAfter": 50000,
      "status": "SUCCESS",
      "createdAt": "2026-05-30T15:20:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2
}
```
 
Validation / Business Rules
 
- 상품 도메인 서버에서만 호출.
- 동일 `productId`로 중복 생성 불가.

```

Validation / Business Rules

- 인증된 사용자 본인의 거래 내역만 조회 가능.
- `from`, `to` 미지정 시 기본 조회 범위는 현재 시각 기준 최근 1개월.
- `from`이 `to`보다 늦을 경우 400 응답.
- `createdAt` 기준 최신순 정렬.
- 월렛 거래(`payment_history`)를 반환.
- 인증 정보는 `@LoginUser` 어노테이션을 통해 컨트롤러 파라미터로 `CurrentUser` 객체로 주입됩니다.

---

### 펀딩 결제

```
POST /api/payment/funding
```

Auth Required: **X** (주문 도메인 서버에서 호출; Kafka로 전달 시 미 사용)

Request Body

| 필드            | 타입     | 필수 | 설명                              |
|---------------|--------|-----|---------------------------------|
| `orderId`     | Long   | Y   | 주문 ID                           |
| `memberId`    | Long   | Y   | 후원자 ID                          |
| `productId`   | Long   | Y   | 상품 ID                           |
| `amount`      | Long   | Y   | 결제 금액                           |
| `paymentType` | Enum   | Y   | 결제 유형 (`INSTANT` \| `RESERVED`) |

Response Body

```json
{
  "orderId": 500,
  "memberId": 1,
  "productId": 100,
  "amount": 10000,
  "paymentType": "INSTANT",
  "tranSeqNo": "018e1234-abcd-7xxx-xxxx-xxxxxxxxxxxx"
}
```

Validation / Business Rules

- 주문 도메인 서버에서만 호출.
- `INSTANT`: 월렛 잔액 즉시 차감 → 상품 가상계좌 적립.
- `RESERVED`: 결제 예약만 등록 (잔액 차감 없음).
- 월렛 잔액 부족 시 결제 실패.
- 비관적 락으로 동시성 제어.
- `tranSeqNo` 중복 검증.
- `tranSeqNo`는 UUID v7 사용.

---

### 수동 환불

```
POST /api/payment/refund
```

Auth Required: **O**

Request Body

| 필드        | 타입   | 필수 | 설명    |
|-----------|------|-----|-------|
| `orderId` | Long | Y   | 주문 ID |

Response Body

```json
{
  "orderId": 500,
  "refundAmount": 10000,
  "balance": 60000
}
```

Validation / Business Rules

- 인증된 사용자 본인만 환불 요청 가능.
- 펀딩 마감 전까지만 수동 환불 가능.
- `funding_payment` 상태 Lock 후 REFUNDED 전환 → 월렛 복원.
- 배치 환불과 수동 환불 동시 요청 시 이중 환불 방지.

---

### [가상 금융망] 계좌 실명 조회 + 1원 송금

```
POST /api/banks/accounts
```

Auth Required: **X**

Request Body

| 필드              | 타입     | 필수 | 설명     |
|------------------|--------|-----|--------|
| `bankCode`       | String | Y   | 은행 코드   |
| `accountNumber`  | String | Y   | 계좌 번호  |
| `holderName`     | String | Y   | 예금주 이름  |

Response Body

```json
{
  "accountNumber": "1234567890",
  "holderName": "홍길동"
}
```

Validation / Business Rules

- 계좌 실명 조회 후 성공 시 해당 계좌로 1원 송금 및 인증 코드 발행.
- 존재하지 않는 계좌일 경우 실패 처리.

---

### [가상 금융망] 인증 코드 조회

```
POST /api/banks/accounts/verify
```

Auth Required: **X**

Request Body

| 필드              | 타입     | 필수 | 설명     |
|------------------|--------|-----|--------|
| `accountNumber`  | String | Y   | 계좌 번호  |
| `code`           | String | Y   | 인증 코드  |

Response Body

```json
{
  "verified": true
}
```

Validation / Business Rules

- 인증 코드 일치 시 `verified: true` 반환.
- 불일치 시 `verified: false` 반환.

---

### [가상 금융망] 계좌 출금 (페이 충전)

```
POST /api/banks/withdraw
```

Auth Required: **X**

Request Body

| 필드              | 타입     | 필수 | 설명       |
|------------------|--------|-----|----------|
| `accountNumber`  | String | Y   | 계좌 번호    |
| `amount`         | Long   | Y   | 출금 금액    |
| `tranSeqNo`      | String | Y   | 거래 고유 번호 |

Response Body

```json
{
  "tranSeqNo": "018e1234-abcd-7xxx-xxxx-xxxxxxxxxxxx",
  "amount": 10000
}
```

Validation / Business Rules

- 계좌 잔액 확인 후 출금 처리.
- 잔액 부족 시 실패 처리.
- 처리 결과 동기 응답.

---

### [가상 금융망] 계좌 입금 (원화 환불)

```
POST /api/banks/deposit
```

Auth Required: **X**

Request Body

| 필드              | 타입     | 필수 | 설명       |
|------------------|--------|-----|----------|
| `accountNumber`  | String | Y   | 계좌 번호    |
| `amount`         | Long   | Y   | 입금 금액    |
| `tranSeqNo`      | String | Y   | 거래 고유 번호 |

Response Body

```json
{
  "tranSeqNo": "018e1234-abcd-7xxx-xxxx-xxxxxxxxxxxx",
  "amount": 10000
}
```

Validation / Business Rules

- 계좌 내 `timeOut` 필드로 타임아웃 시뮬레이션.
- 타임아웃 발생 시 응답 없음 → Pay 서버가 보상 트랜잭션 실행.

---

### [가상 금융망] 거래 코드 조회

```
GET /api/banks/transactions/{tranSeqNo}
```

Auth Required: **X**

Request Body: 없음

Response Body

```json
{
  "tranSeqNo": "018e1234-abcd-7xxx-xxxx-xxxxxxxxxxxx",
  "exists": true
}
```

Validation / Business Rules

- `tranSeqNo` 기반으로 거래 존재 여부 확인.
- 존재할 경우 `exists: true` → 정상 처리로 응답.
- 존재하지 않을 경우 `exists: false` → 미처리로 응답.

---