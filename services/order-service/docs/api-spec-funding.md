## Funding 도메인

### 엔드포인트 목록

#### 클라이언트용

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/fundings` | O (Creator) | 펀딩 생성 |

---

### 1. 펀딩 생성

```
POST /api/fundings
```

**Auth Required:** O (Creator 권한)

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | String | Y | 펀딩 제목 (max 100) |
| `goalAmount` | Long | Y | 목표 금액 |
| `startAt` | DateTime | Y | 펀딩 시작일 |
| `holdTo` | DateTime | Y | 펀딩 종료일 |
| `payAt` | DateTime | Y | 결제 예정일 (펀딩 성공 시 일괄 결제일) |
| `rewards` | Object[] | Y | 리워드 목록 |
| `rewards[].name` | String | Y | 리워드 명 (max 50) |
| `rewards[].description` | String | N | 리워드 설명 (max 200) |
| `rewards[].price` | Integer | Y | 리워드 가격 |
| `rewards[].limitQty` | Integer | Y | 제한 수량 |
| `rewards[].shippingCharge` | Integer | Y | 배송비 |
| `rewards[].badgeType` | String | N | `EARLY_BIRD` / `SUPER_EARLY_BIRD` / `ULTRA_EARLY_BIRD` |
| `rewards[].offerAt` | DateTime | Y | 배송 예정일 |

#### Response Body

```json
{
  "status": 201,
  "data": {
    "fundingId": 407718,
    "title": "제습기",
    "creatorId": 2761944,
    "status": "SCHEDULED",
    "goalAmount": 5000000,
    "currentAmount": 0,
    "participationCnt": 0,
    "startAt": "2025-07-01T00:00:00",
    "holdTo": "2025-07-31T23:59:59",
    "payAt": "2025-08-05T00:00:00",
    "isSuccess": null,
    "accountId": "ACC-20250701-407718",
    "rewards": [
      {
        "rewardId": 810715,
        "name": "[울트라얼리버드] 제습기",
        "price": 138000,
        "limitQty": 370,
        "remainQty": 370,
        "shippingCharge": 0,
        "badgeType": "ULTRA_EARLY_BIRD",
        "offerAt": "2025-08-20T00:00:00"
      }
    ],
    "createdAt": "2025-06-15T10:00:00"
  }
}
```

#### Validation / Business Rules
- 미인증된 사용자 호출: 401 Unauthorized
- Creator 권한 없는 사용자: 403 Forbidden
- `goalAmount < 1`: 400 Bad Request
- `startAt < now()`: 400 Bad Request
- `startAt >= holdTo`: 400 Bad Request
- `holdTo >= payAt`: 400 Bad Request (결제일은 종료 후여야 함)
- `rewards.size() < 1`: 400 Bad Request
- `rewards[].price < 1`: 400 Bad Request
- `rewards[].limitQty < 1`: 400 Bad Request
- `rewards[].shippingCharge < 0`: 400 Bad Request

#### 내부 처리 시퀀스

```
1. Validation (입력값 검증)
2. Funding 레코드 생성 (status: SCHEDULED, currentAmount: 0, participationCnt: 0, isSuccess: null)
3. Reward 레코드 생성 (remainQty = limitQty로 초기화, funding_id 연결)
4. 펀딩 계좌 생성 요청 (HTTP → Payment Service)
   - 요청: { fundingId, creatorId, goalAmount }
   - 응답: { accountId }
5-a. 계좌 생성 성공 → accountId 저장, 201 응답
5-b. 계좌 생성 실패 → Funding/Reward 롤백, 500 응답
```

#### 펀딩 상태 전이 규칙

```
SCHEDULED → OPEN   (startAt 도래, 스케줄러)
OPEN → END         (holdTo 도래, 스케줄러)
END 시점에 isSuccess 판정:
  - currentAmount >= goalAmount → isSuccess = true → payAt에 일괄 결제 트리거
  - currentAmount < goalAmount  → isSuccess = false → 전체 환불 처리
```
---

## 예외 에러 코드
| 상황 | HTTP Status | Error Code | 메시지 |
|------|-------------|------------|--------|
| 시작일이 현재보다 과거 (startAt < now) | 400 | INVALID_START_DATE | "시작일은 현재 시각 이후여야 합니다." | 1 |
| 시작일이 종료일 이후 (startAt >= holdTo) | 400 | INVALID_DATE_RANGE | "종료일은 시작일 이후여야 합니다." | 1 |
| 결제일이 종료일 이전 (payAt <= holdTo) | 400 | INVALID_PAY_DATE | "결제 예정일은 펀딩 종료일 이후여야 합니다." | 1 |
| 펀딩 계좌 생성 실패 (Payment Service 오류) | 500 | ACCOUNT_CREATION_FAILED | "펀딩 계좌 생성에 실패했습니다." | 1 |

