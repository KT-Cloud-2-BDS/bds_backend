## Funding 도메인

### 엔드포인트 목록

| method | path | auth | 설명 |
| --- | --- | --- | --- |
| GET | `/api/fundings` | X | 펀딩 목록 조회 (상태 필터링 가능) |
| GET | `/api/fundings/{fundingId}` | X | 펀딩 상세 및 리워드 목록 조회 |
| POST | `/api/fundings` | O | 펀딩 생성 (리워드 동시 등록, 메이커 전용) |

---

### 1. 펀딩 목록 조회 (List Fundings)

```
GET /api/fundings
```

**Auth Required:** X

#### Query Parameters

| 필드 | 타입 | 필수 | 설명                                                                 |
| --- | --- | --- |--------------------------------------------------------------------|
| `status` | String | N | 펀딩 상태 필터링 값 (`SCHEDULED`, `ACTIVE`, `HOLDING`,`SUCCESS`, `FAILED`) |

#### Response Body

```json
[
  {
    "id": 1,
    "title": "혁신적인 스마트 텀블러 펀딩",
    "creatorId": 100,
    "status": "ACTIVE",
    "goalAmount": 10000000,
    "currentAmount": 2500000,
    "participationCnt": 50,
    "startAt": "2026-08-01T00:00:00",
    "holdTo": "2026-08-31T23:59:59",
    "isSuccess": null,
    "createdAt": "2026-07-15T10:41:49"
  }
]

```

#### Validation / Business Rules

* `status` 파라미터가 비어있거나 생략된 경우 전체 목록을 반환
* `status` 값이 유효하지 않은 `FundingStatus`: 400 Bad Request



---

### 2. 펀딩 상세 조회 (Get Funding Detail)

```
GET /api/fundings/{fundingId}
```

**Auth Required:** X

#### Path Variables

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `fundingId` | Long | Y | 펀딩 고유 ID |

#### Response Body

```json
{
  "id": 1,
  "title": "혁신적인 스마트 텀블러 펀딩",
  "creatorId": 100,
  "status": "ACTIVE",
  "goalAmount": 10000000,
  "currentAmount": 2500000,
  "participationCnt": 50,
  "startAt": "2026-08-01T00:00:00",
  "holdTo": "2026-08-31T23:59:59",
  "payAt": "2026-09-01T00:00:00",
  "isSuccess": null,
  "createdAt": "2026-07-15T10:41:49",
  "updatedAt": "2026-07-15T10:41:49",
  "rewards": [
    {
      "id": 10,
      "name": "스마트 텀블러 얼리버드",
      "description": "얼리버드 혜택 한정 수량 제공",
      "limitQty": 100,
      "remainQty": 50,
      "badgeType": "EARLY_BIRD",
      "price": 50000,
      "offerAt": "2026-09-10T00:00:00",
      "shippingCharge": 3000
    }
  ]
}

```

#### Validation / Business Rules

* 유효하지 않은 `fundingId`: 404 Not Found

---

### 3. 펀딩 생성 (Create Funding)

```
POST /api/fundings
```

**Auth Required:** O (`MAKER` 권한 필요)

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `title` | String | Y | 펀딩 제목 (공백 불가) |
| `goalAmount` | Long | Y | 목표 금액 (양수만 허용) |
| `startAt` | String (ISO-8601) | Y | 펀딩 시작 일시 |
| `holdTo` | String (ISO-8601) | Y | 펀딩 종료 일시 |
| `payAt` | String (ISO-8601) | Y | 결제 예정 일시 |
| `rewards` | Object[] | Y | 생성할 리워드 목록 (최소 1개 이상 필수) |
| `rewards[].name` | String | Y | 리워드 이름 (공백 불가) |
| `rewards[].description` | String | N | 리워드 상세 설명 |
| `rewards[].limitQty` | Integer | Y | 리워드 제한 수량 (양수만 허용) |
| `rewards[].badgeType` | String | N | 배지 타입 (예: `EARLY_BIRD`, `ULTRA_EARLY_BIRD` 등) |
| `rewards[].price` | Long | Y | 리워드 단가 (양수만 허용) |
| `rewards[].offerAt` | String (ISO-8601) | Y | 리워드 제공 예정 일시 |
| `rewards[].shippingCharge` | Long | Y | 배송비 (양수만 허용) |

##### Request Body Example

```json
{
  "title": "혁신적인 스마트 텀블러 펀딩",
  "goalAmount": 10000000,
  "startAt": "2026-08-01T00:00:00",
  "holdTo": "2026-08-31T23:59:59",
  "payAt": "2026-09-01T00:00:00",
  "rewards": [
    {
      "name": "스마트 텀블러 얼리버드",
      "description": "얼리버드 혜택 한정 수량 제공",
      "limitQty": 100,
      "badgeType": "EARLY_BIRD",
      "price": 50000,
      "offerAt": "2026-09-10T00:00:00",
      "shippingCharge": 3000
    }
  ]
}

```

#### Response Body

```json
{
  "fundingId": 1,
  "title": "혁신적인 스마트 텀블러 펀딩",
  "status": "SCHEDULED",
  "startAt": "2026-08-01T00:00:00",
  "holdTo": "2026-08-31T23:59:59",
  "createdAt": "2026-07-15T10:41:49"
}

```

#### Validation / Business Rules

* 요청자의 역할(`role`)이 `MAKER`가 아닐 경우: 403 Forbidden
* `startAt` < now: 400 Bad Request
* `holdTo` <= `startAt`: 400 Bad Request
* `payAt` < `holdTo`: 400 Bad Request
* **스케줄러 등록**: 생성 성공 시 `fundingTaskScheduler`를 통해 종료 시점(`holdTo`)에 성공 여부를 판단하는 스케줄이 자동으로 등록됨.


--

## 예외 에러 코드
| 상황            | HTTP Status | Error Code         | 메시지 | 사용 API |
|---------------|-------------|--------------------| --- |--------|
| 유효 펀딩 없음      | 400         | FUNDING_NOT_FOUND  | "요청한 펀딩을 찾을 수 없습니다" | 2      |
| 메이커 권한 없음     | 403         | ACCESS_DENIED      | "접근 권한이 없습니다" | 3      |
| 펀딩 시작일 유효성 실패 | 400         | INVALID_START_DATE | "시작일은 현재 시각 이후여야 합니다" | 3      |
| 펀딩 종료일 유효성 실패 | 400         | INVALID_DATE_RANGE | "종료일은 시작일 이후여야 합니다" | 3      |
| 결제 예정일 유효성 실패 | 400         | INVALID_PAY_DATE   | "결제 예정일은 펀딩 종료일 이후여야 합니다" | 3      |