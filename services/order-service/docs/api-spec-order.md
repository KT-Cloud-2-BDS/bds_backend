## Order 도메인

### 주문 플로우
```text
[리워드 선택] → POST /api/orders/billing (주문서 미리보기)
     ↓
[결제하기 클릭] → POST /api/orders (주문 생성 + 재고 차감 + 결제 호출)
     ↓
[결제 처리 완료] → POST /api/internal/orders/{orderId}/status (PG 콜백)
```

### 엔드포인트 목록

#### A. 클라이언트용

| method  | path                         | auth | 설명                    |
|---------|------------------------------|------|-----------------------|
| POST    | `/api/orders/billing`        | O    | 주문서 생성 (정보 조회, DB X)  |
| POST    | `/api/orders`                | O    | 주문 생성 (재고 차감 + 결제 호출) |
| GET     | `/api/orders`                | O    | 내 주문 목록 조회            |
| GET     | `/api/orders/{orderId}`      | O    | 주문 상세 조회              |
| PATCH   | `/api/orders/{orderId}/cancel` | O    | 사용자 주문 취소             |

#### B. 시스템용 (서비스 간 내부 통신)

| method  | path                                    | auth          | 설명                                 |
|---------|-----------------------------------------|---------------|------------------------------------|
| POST    | `/api/internal/orders/{orderId}/status` | API Key / 내부망 | 결제 콜백 수신 (PAID/CANCELLED/REFUNDED) |

---


### 1. 주문서 생성 (Billing Preview)

```
POST /api/orders/billing
```

**Auth Required:** O

#### Request Body

| 필드                | 타입       | 필수 | 설명        |
|-------------------|----------|----|-----------|
| `fundingId`       | Long     | Y  | 펀딩 ID     |
| `isReservedOrder` | Boolean  | N  | 예약 주문 여부  |
| `rewards`         | Object[] | Y  | 주문 리워드 목록 |
| `rewards[].id`    | Long     | Y  | 리워드 ID    |
| `rewards[].qty`   | Integer  | Y  | 수량        |

#### Response Body

```json
{
    "memberId": 1,
    "rewards": [
      {
        "id": 192412,
        "qty": 2,
        "name": "선크림 4병 + 쿠션 퍼프 1개",
        "amount": 182400,
        "badgeType": "ULTRA_EARLY_BIRD",
        "shippingCharge": 5000
      }
    ],
    "rewardAmount": 182400,
    "totalShippingCharge": 5000,
    "totalBillingAmount": 187400
}
```

#### Validation / Business Rules
- `memberId`는 Gateway에서 decrypt되어 헤더에 포함된 값 사용
- 미리보기 전용으로 DB 저장 X
- `rewards.size() < 1`: 400 Bad Request
- `qty < 1`: 400 Bad Request
- 존재하지 않는 리워드: 404 Not Found
- 펀딩 기간 외 요청: 403 Forbidden
- 리워드 재고 불충족: 409 Conflict
- 동일 리워드 중복: 400 Bad Request

---

### 2. 주문 생성

```
POST /api/orders
```

**Auth Required:** O

#### Request Body

| 필드          | 타입       | 필수 | 설명       |
|-------------|----------|----|----------|
| `orderId`   | Long     | Y  | 주문 ID    |
| `fundingId` | Long     | Y  | 펀딩 ID    |
| `isNowPay`  | Boolean  | N  | 예약 주문 여부 |

#### Response Body

```json
{
    "memberId": 1,
    "orderNo": "ORD-20250201-00001",
    "totalBillingAmount": 187400,
    "orderStatus": "PAYING",
    "payRequestedAt": "2025-02-01T14:30:00"
}
```

#### Validation / Business Rules
- `memberId`는 Gateway에서 decrypt되어 헤더에 포함된 값 사용
- 재고 차감: CAS(Compare-And-Swap) Update로 동시성 처리
- 재고 부족: 409 Conflict
- 펀딩 기간 재확인 (billing → 주문 사이 마감 가능): 403 Forbidden
- 결제 실패 시 롤백: 재고 복구 + 주문 상태 `CANCELLED`

#### 내부 처리 시퀀스

```
1. Validation (기간, 수량 제한, 멱등성 확인)
2. 재고 차감 (CAS Update)
3. 주문 레코드 생성 (상태: PENDING)
4. 결제 요청 (HTTP → Payment Service)
5-a. 결제 성공 → 주문 상태 PAID, 응답 반환
5-b. 결제 실패 → 재고 복구, 주문 상태 FAILED
```

---

### 3. 내 주문 목록 조회

```
GET /api/orders
```

**Auth Required:** O

#### Request Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `page` | Integer | N | 페이지 번호 (default: 0) |
| `size` | Integer | N | 페이지 크기 (default: 20) |

#### Response Body

```json
{
    "content": [
      {
        "orderNo": "ORD-20250201-00001",
        "orderStatus": "PAID",
        "fundingDate": "2025-01-10T04:54:42+09:00",
        "title": "선글라스",
        "hostId": 20112,
        "isEnded": false,
        "billingAmount": 51800,
        "updatedAt": "2025-01-12T17:00:00+09:00",
        "isFundingSucceeded": true
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1
}
```

#### Validation / Business Rules
- `memberId`는 Gateway에서 decrypt되어 헤더에 포함된 값 사용
- 미인증된 사용자 호출: 401 Unauthorized
- 본인 주문만 조회
- 최신순 정렬 (default)
- 페이지네이션 적용

---

### 4. 주문 상세 조회

```
GET /api/orders/{orderId}
```

**Auth Required:** O

#### Path Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `orderId` | Long | Y | 주문 ID |

#### Response Body

```json
{
    "orderNo": "ORD-20250201-00001",
    "fundingDate": "2025-01-10T04:54:42+09:00",
    "title": "선글라스",
    "hostId": 21103,
    "isEnded": false,
    "isFundingSucceeded": true,
    "memberId": 1,
    "orderStatus": "PAID",
    "rewards": [
      {
        "id": 192412,
        "qty": 2,
        "name": "선크림 4병 + 쿠션 퍼프 1개",
        "amount": 182400,
        "badgeType": "ULTRA_EARLY_BIRD",
        "shippingCharge": 5000
      }
    ],
    "rewardAmount": 182400,
    "totalShippingCharge": 5000,
    "totalBillingAmount": 187400,
    "updatedAt": "2025-02-01T14:30:00",
    "cancelledAt": null
}
```

#### Validation / Business Rules
- `memberId`는 Gateway에서 decrypt되어 헤더에 포함된 값 사용
- 타인 주문 접근: 403 Forbidden
- 존재하지 않는 orderId: 404 Not Found

---

### 5. 사용자 주문 취소

```
PATCH /api/orders/{orderId}/cancel
```

**Auth Required:** O

#### Path Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `orderId` | Long | Y | 주문 ID |

#### Response Body

```json
{
    "orderNo": "ORD-20250201-00001",
    "orderStatus": "CANCELLED",
    "cancelledAt": "2025-02-03T10:00:00",
    "refundStatus": "REFUND_REQUESTED"
}
```

#### Validation / Business Rules
- `memberId`는 Gateway에서 decrypt되어 헤더에 포함된 값 사용
- 타인 주문 접근: 403 Forbidden
- 존재하지 않는 orderId: 404 Not Found
- 취소 불가 상태 (PAID 상태만 취소 가능): 400 Bad Request
- 이미 취소된 주문 재취소: 409 Conflict
- 재고 복구
- 환불 요청 (HTTP → Payment Service), 실패 시 수동 처리 큐 적재

---

### 6. 결제 콜백 수신 (내부 API)

```
POST /api/internal/orders/{orderId}/status
```

**Auth:** API Key / 내부망

#### Path Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `orderId` | Long | Y | 주문 ID |

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `paymentStatus` | String | Y | `PAID` / `CANCELLED` / `REFUNDED` / `FAILED` |
| `paymentId` | String | Y | 결제 서비스 결제 ID |
| `cancelReason` | String | 조건부 | `TIMEOUT` / `PAYMENT_FAILED` / `USER_CANCEL` |
| `paidAt` | DateTime | 조건부 | 결제 완료 시각 |
| `cancelledAt` | DateTime | 조건부 | 취소 완료 시각 |
| `refundedAt` | DateTime | 조건부 | 환불 완료 시각 |

#### 조건부 필수 규칙

| paymentStatus | 필수 필드 |
|---------------|-----------|
| `PAID` | `paidAt` |
| `CANCELLED` | `cancelledAt`, `cancelReason` |
| `REFUNDED` | `refundedAt`, `cancelReason` |
| `FAILED` | `cancelReason` |


#### Response Body

```json
{
  "status": 200,
  "data": {
    "orderNo": "ORD-20250201-00001",
    "orderStatus": "PAID",
    "updatedAt": "2025-02-01T14:30:01"
  }
}
```

#### Validation / Business Rules
- API Key 또는 내부망 인증 필수: 401 Unauthorized
- 존재하지 않는 orderId: 404 Not Found
- 유효하지 않은 상태 전이 (e.g., FAILED → PAID): 409 Conflict
- 조건부 필수 필드 누락: 400 Bad Request
- `cancelReason`이 허용 값 외: 400 Bad Request
- `CANCELLED` / `REFUNDED` 수신 시 재고 복구 처리
- `FAILED` 수신 시 재고 복구 + 주문 상태 FAILED 처리



#### 주문 상태 전이 규칙

```
# 즉시 결제
PENDING → PAYING      (결제 요청)

# 예약 결제
RESERVED → PAYING     (펀딩 성공 후 결제 트리거)
RESERVED → CANCELLED  (펀딩 실패 / 사용자 취소 / 타임아웃)

# 공통
PAYING → PAID         (결제 성공)
PAYING → CANCELLED       (결제 실패)
PAID → CANCELLED      (사용자 취소)
CANCELLED → REFUNDED  (환불 완료)
```
---
## 예외 에러 코드
| 상황 | HTTP Status | Error Code | 메시지 | 사용 API |
|------|-------------|------------|--------|----------|
| 펀딩 기간 외 요청 | 403 | FUNDING_NOT_OPEN | "현재 펀딩 기간이 아닙니다." | 1, 2 |
| 존재하지 않는 리워드 조회 | 404 | REWARD_NOT_FOUND | "존재하지 않는 리워드입니다." | 1, 2 |
| 존재하지 않는 주문 조회 | 404 | ORDER_NOT_FOUND | "존재하지 않는 주문입니다." | 4, 5, 6 |
| 동일 리워드 중복 선택 | 400 | DUPLICATE_REWARD | "동일한 리워드가 중복 선택되었습니다." | 1, 2 |
| 해당 펀딩에 속하지 않는 리워드 선택 | 400 | REWARD_NOT_BELONG_TO_FUNDING | "해당 펀딩에 속하지 않는 리워드입니다." | 1, 2 |
| 리워드 재고 부족 | 409 | REWARD_SOLD_OUT | "해당 리워드는 품절되었습니다." | 1, 2 |
| 재고 차감 실패 (CAS 충돌) | 409 | STOCK_CONFLICT | "재고 차감에 실패했습니다. 다시 시도해주세요." | 2 |
| 취소 불가 상태에서 취소 요청 | 400 | ORDER_NOT_CANCELLABLE | "취소할 수 없는 주문 상태입니다." | 5 |
| 이미 취소된 주문 재취소 | 409 | ORDER_ALREADY_CANCELLED | "이미 취소된 주문입니다." | 5 |
| 유효하지 않은 상태 전이 | 409 | INVALID_STATUS_TRANSITION | "유효하지 않은 주문 상태 변경입니다." | 6 |
