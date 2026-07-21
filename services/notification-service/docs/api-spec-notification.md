# 알림 도메인

## 엔드포인트 목록

| method | path | auth required | 설명 |
|--------|------|---------------|------|
| GET | /api/notifications/connect | O | SSE 연결 (알림 구독) |
| GET | /api/notifications | O | 알림 목록 조회 + 전체 읽음 처리 |
| GET | /api/notifications/unread-count | O | 읽지 않은 알림 수 조회 |
| POST | /api/notifications/subscriptions/{targetType}/{targetId} | O | 알림 구독 등록 |
| DELETE | /api/notifications/subscriptions/{targetType}/{targetId} | O | 알림 구독 해지 |
| POST | /api/notifications/fcm-token | O | FCM 토큰 저장 |
| DELETE | /api/notifications/fcm-token | O | FCM 토큰 삭제 |

---

## SSE 연결

```
GET /api/notifications/connect
```

Auth Required: **O**

Response: `text/event-stream`

> JSON Response Body로 내려오는 것이 아니라, 연결이 유지되는 동안 서버가 아래 형식의 이벤트를 스트림으로 밀어넣는다.

**연결 확인 이벤트** — 연결 직후 서버가 즉시 전송

```
event: connect
data: connected
```

**알림 이벤트** — 알림 발생 시마다 전송

```
event: notification
data: {"notificationId": 1, "type": "FUNDING_START", "message": "찜한 상품의 펀딩이 시작되었습니다.", "targetId": 101, "createdAt": "2026-04-21T09:00:00Z"}
```

**Validation / Business Rules**
- 연결 성공 시 즉시 `connect` 이벤트를 전송한다. (프록시/브라우저 연결 유지 목적)
- 서버 측 timeout은 30분이며, 만료 시 클라이언트가 자동 재연결을 시도한다.
- 연결 종료(탭 닫기, 네트워크 끊김) 시 서버에서 해당 SseEmitter를 자동 제거한다.

---

## 알림 목록 조회

```
GET /api/notifications
```

Auth Required: **O**

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `page` | Integer | N | 페이지 번호 (default: 0) |
| `size` | Integer | N | 페이지 크기 (default: 20) |

**Response Body**

```json
{
  "notifications": [
    {
      "notificationId": 1,
      "type": "FUNDING_START",
      "message": "찜한 상품의 펀딩이 시작되었습니다.",
      "targetId": 101,
      "isRead": true,
      "createdAt": "2026-04-21T09:00:00Z"
    }
  ],
  "totalCount": 15,
  "unreadCount": 0
}
```

**Validation / Business Rules**
- 목록 조회 시점에 미읽음 알림 전체를 읽음 상태로 일괄 업데이트한다.
- `type` 값: `FUNDING_START` | `FUNDING_SUCCESS` | `FUNDING_FAIL` | `PROMOTION`
- `targetId`: 알림과 연관된 상품 ID

---

## 읽지 않은 알림 수 조회

```
GET /api/notifications/unread-count
```

Auth Required: **O**

**Response Body**

```json
{
  "unreadCount": 3
}
```

**Validation / Business Rules**
- 알림 아이콘 배지 표시용으로, 목록 조회 없이 카운트만 가져올 때 사용한다.

---

## 알림 구독 등록

```
POST /api/notifications/subscriptions/{targetType}/{targetId}
```

Auth Required: **O**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `targetType` | String | 구독 타입 (`PRODUCT` \| `PROMOTION`) |
| `targetId` | Long | 구독 대상 ID |

**Response Body**

```json
{
  "targetType": "PRODUCT",
  "targetId": 101,
  "subscribed": true
}
```

**Validation / Business Rules**
- 이미 구독 중인 경우 `409 Conflict` 반환.

---

## 알림 구독 해지

```
DELETE /api/notifications/subscriptions/{targetType}/{targetId}
```

Auth Required: **O**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `targetType` | String | 구독 타입 (`PRODUCT` \| `PROMOTION`) |
| `targetId` | Long | 구독 대상 ID |

**Response Body**

```
204 No Content
```

**Validation / Business Rules**
- 구독 정보가 없는 경우 `404 Not Found` 반환.
- 소프트 딜리트 방식으로 처리된다.

---

## FCM 토큰 저장

```
POST /api/notifications/fcm-token
```

Auth Required: **O**

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `token` | String | Y | 브라우저에서 발급된 FCM 토큰 |

**Response**

```
204 No Content
```

**Validation / Business Rules**
- 로그인 후 브라우저에서 FCM 토큰 발급 시 즉시 호출한다.
- 동일 유저의 기존 토큰이 있으면 덮어쓴다. (브라우저마다 토큰이 다를 수 있으므로 user_id + token으로 upsert)
- SSE Emitter가 없는 경우 이 토큰으로 FCM 알림을 발송한다.

---

## FCM 토큰 삭제

```
DELETE /api/notifications/fcm-token
```

Auth Required: **O**

**Response**

```
204 No Content
```

**Validation / Business Rules**
- 로그아웃 시 호출하여 해당 브라우저의 토큰을 삭제한다.
- 삭제하지 않으면 로그아웃 후에도 FCM 알림이 전달될 수 있다.
- 토큰이 존재하지 않는 경우 `404 Not Found` 반환.
