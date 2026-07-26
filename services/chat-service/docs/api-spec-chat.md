# Chat 도메인

## 엔드포인트 목록

| method | path                                         | auth required | 설명                           |
|--------|----------------------------------------------|-----------|------------------------------|
| POST   | `/api/chat/Inquiries`                        | O         | 1:1 문의 채팅방 생성                |
| GET    | `/api/chat/Inquiries`                        | O         | 내 참여 문의 채팅방 목록 조회            |
| GET    | `/api/chat/Inquiries/{roomId}`               | O         | 1:1 문의 채팅방 상세 조회             |
| DELETE | `/api/chat/Inquiries/{roomId}/members/me`    | O         | 1:1 문의 채팅방 나가기               |
| DELETE | `/api/chat/rooms/{roomId}/close`             | O         | 공개 채팅방 삭제                    |
| POST   | `/internal/chat/fundings/{productId}`        | X         | 펀딩 제품 생성시 공개 채팅방 자동 생성 (시스템) |
| GET    | `/api/chat/fundings/{productId}`             | X         | 공개 채팅방 조회                    |
| POST   | `/api/chat/fundings/{roomId}/ban`            | O         | 공개 채팅방 사용자 BAN               |
| DELETE | `/api/chat/fundings/{roomId}/ban/{targetId}` | O         | 공개 채팅방 사용자 BAN 해제            |
| GET    | `/api/chat/rooms/messages`                   | O         | 채팅 이력 조회                     |
| GET    | `/api/chat/Inquiries/{roomId}/messages`      | O         | 1:1 문의 채팅방 메시지 조회            |
| GET    | `/api/chat/fundings/{roomId}/messages`       | O         | 공개 채팅방 메시지 조회                |
| DELETE | `/api/chat/messages/{messageId}`             | O         | 메시지 삭제(soft delete)          |
| WS     | `/ws/chat`                                   | X         | Websocket 연결                 |
| SUB    | `/topic/chat.room.{roomId}`                  | O/X       | 채팅방 구독 (메시지/이벤트)            |
| SUB    | `/topic/chat.room.{roomId}.read`             | O/X       | 채팅방 읽음 이벤트 구독               |
| UNSUB  | `subscription-id`                            | O/X       | 채팅방 구독 취소                    |
| PUB    | `/app/chat/send/{roomId}`                    | O         | 메시지 전송                       |
| PUB    | `/app/chat/read/{roomId}`                    | O         | 읽음 상태 갱신                     |
| PUB    | `/app/auth/refresh`                          | O         | 액세스 토큰 갱신                    |

---

## 1:1 문의 채팅방 생성

```
POST /api/chat/Inquiries
```

Auth Required: **O**

Request Body

| 필드           | 타입      | 필수 | 설명       |
|--------------|---------|---|----------|
| `productId`  | Long*   | Y | 펀딩 제품 id |

Response Body

```json
{
    "roomId": 201,
    "type": "INQUIRY",
    "productId": 101,
    "participants": [55],
    "createdBy": 9,
    "createdAt": "2026-04-21T09:00:00Z",
    "status": "ACTIVE"
}
```

Validation / Business Rules

- 채팅방은 상품 생성자(seller) 와 요청 사용자(buyer) 간에만 생성된다.
- productId는 반드시 존재하는 펀딩 상품이어야 한다.
- 동일한 (productId + buyerId) 조합에 대해서는 하나의 채팅방만 생성된다.
- 이미 존재하는 경우 409 Conflict를 반환한다.
- 채팅방 type은 INQUIRY로 고정된다.
- seller ID는 클라이언트로부터 직접 받지 않는다. 해당 product의 FUNDING 채팅방 creator_id를 seller로 간주하여 chat-service 내부에서 조회한다. 따라서 FUNDING 채팅방이 존재하지 않는 product에 대해서는 INQUIRY 채팅방 생성이 불가능하며 404를 반환한다.
- 생성 시 buyer와 seller 모두 ACTIVE 멤버로 즉시 추가된다.
---

## 내 참여 문의 채팅방 목록 조회

```
GET /api/chat/Inquiries
```

Auth Required: **O**

Response Body

```json
{
  "rooms": [
      {
        "roomId": 201,
        "type": "INQUIRY",
        "productId": 101,
        "participants": [55],
        "createdBy": 9,
        "createdAt": "2026-04-21T09:00:00Z",
        "lastMessage": {
          "messageId": 981,
          "senderId": 55,
          "content": "안녕하세요",
          "isDeleted": false,
          "createdAt": "2026-04-27T14:22:15Z"
        },
        "unreadCount": 2,
        "status": "ACTIVE"
      }
  ],
  "nextCursor": null,
  "hasNext": false,
  "totalCount": 1
}
```

Validation / Business Rules

- 사용자는 자신이 참여중인 모든 1:1 채팅방에 대한 정보를 가져올 수 있다.
- 채팅방을 최대 20개까지 한번에 가져올 수 있다.
- 참여중인 채팅방이 20개가 넘어갈 시 "hasNext" 필드의 값이 true가 되며 다음으로 읽어야할 채팅방의 roomId가 "nextCursor"로 제공된다.
- 만약 다음으로 읽어올 수 있는 채팅방이 존재하지 않을 경우 "hasNext" 필드의 값은 false, "nextCursor" 필드의 값은 null이 된다.
---
## 1:1 문의 채팅방 상세 조회

```
GET /api/chat/Inquiries/{roomId}
```

Auth Required: **O**

Response Body

```json
{
    "roomId": 201,
    "type": "INQUIRY",
    "productId": 101,
    "participants": [
      {
        "memberId": 55,
        "membership": {
          "status": "ACTIVE",
          "lastReadMessageId": 880,
          "joinedAt": "2026-04-21T09:00:00Z"
        }
      },
      {
        "memberId": 9,
        "membership": {
          "status": "ACTIVE",
          "lastReadMessageId": 870,
          "joinedAt": "2026-04-21T09:00:00Z"
        }
      }
    ],
    "createdBy": 9,
    "createdAt": "2026-04-21T09:00:00Z",
    "lastMessage": {
      "messageId": 981,
      "senderId": 55,
      "content": "안녕하세요",
      "isDeleted": false,
      "createdAt": "2026-04-27T14:22:15Z"
    },
    "myMembership": {
      "status": "ACTIVE",
      "lastReadMessageId": 880,
      "joinedAt": "2026-04-21T09:00:00Z"
    },
    "status": "ACTIVE"
}
```

Validation / Business Rules

- 사용자는 자신이 참여중인 1:1 채팅방의 정보만 가져올 수 있다.
- participants 배열에는 채팅방의 모든 활성 멤버의 id와 membership 상태가 포함된다.
- 사용자 본인의 상태(ACTIVE, LEFT, BANNED)는 myMembership 필드를 통해 별도로 확인할 수 있다.
---

## 1:1 문의 채팅방 나가기

```
DELETE /api/chat/Inquiries/{roomId}/members/me
```

Auth Required: **O**

Path Variable

| 필드       | 타입    | 필수 | 설명     |
|----------|-------|---|--------|
| `roomId` | Long* | Y | 채팅방 id |

Response Body

```json
{
  "roomId": 201,
  "memberId": 55,
  "leftAt": "2026-07-07T12:00:00Z"
}
```

Validation / Business Rules

- 본인이 참여 중인 채팅방만 나갈 수 있으며, 참여 중이 아닌 경우 404를 반환한다.
- 차단(BANNED)된 사용자는 나가기를 수행할 수 없으며 403을 반환한다.
- 상대방이 이미 나간 상태에서 본인이 다시 채팅방을 생성하면 양측 모두 자동으로 재입장 처리된다.
---

## 공개 채팅방 삭제

```
DELETE /api/chat/rooms/{roomId}/close
```

Auth Required: **O**

Path Variable

| 필드       | 타입      | 필수 | 설명     |
|----------|---------|---|--------|
| `roomId` | Long*   | Y | 채팅방 id |

Response Body
```json
{
  "roomId": 981,
  "isDeleted": true,
  "deletedAt": "2026-06-30T11:21:00Z"
}
```


Validation / Business Rules
- 공채 채팅방만을 삭제할 수 있다.
- 본인이 생성한 채팅방만 삭제가 가능하며, 이미 삭제된 채팅방은 삭제할 수 없다.
- 삭제된 채팅방은 복구할 수 없다.
- 해당 채팅방에 구독된 상태에서만 삭제가 가능하다.
- 채팅방 삭제 결과는 해당 채팅방의 모든 구독자에게 실시간으로 반영한다.
---

## 펀딩 제품 생성시 공개 채팅방 자동 생성

```
POST /internal/chat/fundings/{productId}
```

Auth Required: **X**

Path Variable

| 필드          | 타입    | 필수 | 설명       |
|-------------|-------|---|----------|
| `productId` | Long* | Y | 펀딩 제품 id |

Request Body

| 필드          | 타입    | 필수 | 설명            |
|-------------|-------|---|---------------|
| `creatorId` | Long* | Y | 펀딩 채팅방 생성자 id |

Response Body

```json
{
    "roomId": 201,
    "type": "FUNDING",
    "productId": 101,
    "participants": [],
    "createdBy": 9,
    "createdAt": "2026-04-21T09:00:00Z",
    "status": "ACTIVE"
}
```


Validation / Business Rules
- 펀딩 제품이 성공적으로 등록되면 시스템은 해당 제품의 공개 채팅방을 자동으로 생성한다.
- 공개 채팅방은 제품당 하나만 생성되어야 한다.
- 실패시 펀딩 제품 생성자가 직접 생성을 호출 할 수 있다.
---

## 공개 채팅방 조회

```
GET /api/chat/fundings/{productId}
```

Auth Required: **X**

Path Variable

| 필드       | 타입      | 필수 | 설명       |
|----------|---------|---|----------|
| `productId` | Long*   | Y | 펀딩 제품 id |

Response Body

```json
{
    "roomId": 201,
    "type": "FUNDING",
    "productId": 101,
    "participants": [55],
    "createdBy": 9,
    "createdAt": "2026-04-21T09:00:00Z",
    "status": "ACTIVE"
}
```


Validation / Business Rules
- 모든 사용자가 조회가 가능하다
- 삭제된 공개 채팅방의 경우 409 ROOM_ALREADY_CLOSED를 발산한다.
---


## 공개 채팅방 사용자 BAN

```
POST /api/chat/fundings/{roomId}/ban
```

Auth Required: **O**

Path Variable

| 필드       | 타입      | 필수 | 설명     |
|----------|---------|---|--------|
| `roomId` | Long*   | Y | 채팅방 id |

Request Body

| 필드         | 타입     | 필수 | 설명         |
|------------|--------|---|------------|
| `targetId` | Long*  | Y | 차단할 사용자 id |
| `reason`   | String | N | 차단 사유      |

Response Body

```json
{
    "roomId": 301,
    "bannedUserId": 44,
    "status": "ACTIVE"
}
```

Validation / Business Rules
- 차단된 사용자는 채팅 메시지를 전송할 수 없다.
- 해당 펀딩 생성자만 수행할 수 있다.
- 본인을 차단할 수 없으며 이 경우 400을 반환한다.
- 이미 차단된 사용자를 다시 차단할 경우 409를 반환한다.
---

## 공개 채팅방 사용자 BAN 해제

```
DELETE /api/chat/fundings/{roomId}/ban/{targetId}
```

Auth Required: **O**

Path Variable

| 필드         | 타입    | 필수 | 설명           |
|------------|-------|---|--------------|
| `roomId`   | Long* | Y | 채팅방 id       |
| `targetId` | Long* | Y | 해제할 사용자 id   |

Response Body

```json
{
    "roomId": 301,
    "bannedUserId": 44,
    "status": "RELEASED"
}
```

Validation / Business Rules
- 해당 펀딩 생성자만 수행할 수 있다.
- 활성 차단 상태인 사용자만 해제할 수 있으며, 차단 이력이 없거나 이미 해제된 경우 404를 반환한다.
---

## 채팅 이력 조회

```
GET /api/chat/rooms/messages
```

Auth Required: **O**

Query Parameter

| 필드       | 타입    | 필수 | 설명                        |
|----------|-------|---|---------------------------|
| `cursor` | Long* | N | 커서 기반 페이징 기준 messageId (미입력 시 최신부터 조회) |

Response Body

```json
{
  "messages": [
    {
      "messageId": 981,
      "roomId": 101,
      "senderId": 55,
      "content": "안녕하세요",
      "type": "TEXT",
      "isDeleted": false,
      "createdAt": "2026-04-27T14:22:15Z"
    }
  ],
  "nextCursor": null,
  "hasNext": false,
  "totalCount": 1
}
```


Validation / Business Rules
- INQUIRY, FUNDING 등 모든 타입의 채팅방 구분 없이 사용자가 작성한 모든 채팅 이력을 조회한다.
- 자신이 생성한 MESSAGE만 조회할 수 있다.
- 메시지를 최대 20개까지 한번에 가져올 수 있다.
- 가져온 메시지가 20개가 넘어갈 시 "hasNext" 필드의 값이 true가 되며 다음으로 읽어야할 메시지의 id가 "nextCursor"로 제공된다.
- 다음으로 읽어올 수 있는 메시지가 존재하지 않을 경우 "hasNext" 필드의 값은 false, "nextCursor" 필드의 값은 null이 된다.
---
## 1:1 문의 채팅방 메시지 조회

```
GET /api/chat/Inquiries/{roomId}/messages
```

Auth Required: **O**

Path Variable

| 필드       | 타입    | 필수 | 설명     |
|----------|-------|---|--------|
| `roomId` | Long* | Y | 채팅방 id |

Query Parameter

| 필드       | 타입    | 필수 | 설명                        |
|----------|-------|---|---------------------------|
| `cursor` | Long* | N | 커서 기반 페이징 기준 messageId (미입력 시 최신부터 조회) |

Response Body

```json
{
  "messages": [
    {
      "messageId": 981,
      "roomId": 101,
      "senderId": 55,
      "content": "안녕하세요",
      "type": "TEXT",
      "isDeleted": false,
      "createdAt": "2026-04-27T14:22:15Z"
    }
  ],
  "nextCursor": null,
  "hasNext": false,
  "totalCount": 1
}
```


Validation / Business Rules
- INQUIRY 채팅방의 메시지만 조회할 수 있으며, FUNDING 채팅방 roomId를 사용할 경우 404가 반환된다.
- 조회와 동시에 가장 최근 메시지 id가 lastReadMessageId로 갱신된다.
- 해당 채팅방의 활성 멤버(ACTIVE)만 조회할 수 있으며, 아닐 경우 403 FORBIDDEN 에러가 발생한다.
- 메시지를 최대 20개까지 한번에 가져올 수 있다.
- 가져온 메시지가 20개가 넘어갈 시 "hasNext" 필드의 값이 true가 되며 다음으로 읽어야할 메시지의 id가 "nextCursor"로 제공된다.
- 다음으로 읽어올 수 있는 메시지가 존재하지 않을 경우 "hasNext" 필드의 값은 false, "nextCursor" 필드의 값은 null이 된다.
---

## 공개 채팅방 메시지 조회

```
GET /api/chat/fundings/{roomId}/messages
```

Auth Required: **O**

Path Variable

| 필드       | 타입    | 필수 | 설명     |
|----------|-------|---|--------|
| `roomId` | Long* | Y | 채팅방 id |

Query Parameter

| 필드       | 타입    | 필수 | 설명                        |
|----------|-------|---|---------------------------|
| `cursor` | Long* | N | 커서 기반 페이징 기준 messageId (미입력 시 최신부터 조회) |

Response Body

```json
{
  "messages": [
    {
      "messageId": 981,
      "roomId": 101,
      "senderId": 55,
      "content": "안녕하세요",
      "type": "TEXT",
      "isDeleted": false,
      "createdAt": "2026-04-27T14:22:15Z"
    }
  ],
  "nextCursor": null,
  "hasNext": false,
  "totalCount": 1
}
```


Validation / Business Rules
- FUNDING 채팅방의 메시지만 조회할 수 있으며, INQUIRY 채팅방 roomId를 사용할 경우 404가 반환된다.
- 메시지를 최대 20개까지 한번에 가져올 수 있다.
- 가져온 메시지가 20개가 넘어갈 시 "hasNext" 필드의 값이 true가 되며 다음으로 읽어야할 메시지의 id가 "nextCursor"로 제공된다.
- 다음으로 읽어올 수 있는 메시지가 존재하지 않을 경우 "hasNext" 필드의 값은 false, "nextCursor" 필드의 값은 null이 된다.
---
## 메시지 삭제

```
DELETE /api/chat/messages/{messageId}
```

Auth Required: **O**

Path Variable

| 필드       | 타입      | 필수 | 설명     |
|----------|---------|---|--------|
| `messageId` | Long*   | Y | 메시지 id |

Response Body

```json
{
  "messageId": 981,
  "isDeleted": true,
  "deletedAt": "2026-06-30T11:21:00Z"
}
```


Validation / Business Rules
- 본인이 보낸 메시지만 삭제가 가능하며 아닐시 403 FORBIDDEN 에러가 전파된다.
- 이미 삭제된 메시지는 삭제할 수 없다.
- 삭제된 메시지는 복구할 수 없다.
- 해당 채팅방에 구독된 상태에서만 삭제가 가능하다.
- 메시지 삭제 결과는 해당 채팅방의 모든 구독자에게 실시간으로 반영된다.
- 차단된 사용자는 메시지를 삭제할 수 없으며 이 경우 403 에러가 전파된다.
---

## WebSocket(STOMP)

### 연결(CONNECT)

```
WS  /ws/chat   (STOMP over WebSocket)
```

#### CONNECT 헤더

| 헤더       | 필수   | 설명     |
|----------|------|--------|
|Authorization | N | Bearer {accessToken} |
|accept-version | Y |1.1,1.2|
|heart-beat | N |	10000,10000 |


- 인증 실패 시 STOMP ERROR 프레임 후 연결 종료.
- 동일 사용자의 다중 디바이스 접속 허용 (sessionId 별로 분리 관리).

---

### 채팅방 구독

#### 구독 Destination

| 목적지 | 설명 |
|-----|--------|
| `/topic/chat.room.{roomId}` | 해당 방의 메시지 이벤트 |
| `/topic/chat.room.{roomId}.read` | 해당 방의 읽음 이벤트 |


#### SUBSCRIBE 헤더 명세

| 헤더       | 필수   | 설명     |
|----------|------|--------|
| id | Y | subscription id |
| lastMessageId | N | lastMessageId {MessageId} |

#### 구독 요청 예시 (Client -> Server)
```stomp
SUBSCRIBE
id:sub-room-10
destination:/topic/chat.room.10
lastMessageId:532^@
```

#### 구독 성공 응답 예시 (Server -> Client)
```stomp
MESSAGE
subscription-id:<subscriptionId값>
destination:<destination값>
content-type:application/json
content-length:56

{"type":"SUBSCRIBED","destination":"/topic/chat.room.10"}^@
```
Validation / Business Rules
- 공개 채팅방의 경우 회원, 비회원 모두 구독이 가능하다.
- 1:1 채팅방의 경우 해당 채팅방의 멤버만 구독이 가능하며 이를 어길시 403 FORBIDDEN 에러가 전파된다.
- 삭제되거나 존재하지 않은 채팅방에는 구독할 수 없으며 이를 어길시 404 NOT_FOUND 에러가 전파된다.
- 사용자는 구독 성공시 성공 응답을 송신받는다.
- 채팅방 구독 전 실시간 통신 연결이 수립되어야 한다.
- 로그인 사용자의 경우 인증 정보가 연결 세션에 유지되어야 한다.
---


### 채팅방 구독 취소

#### UNSUBSCRIBE 헤더 명세

| 헤더       | 필수   | 설명     |
|----------|------|--------|
| id | Y | subscription id |

#### 구독 요청 예시 (Client -> Server)
```stomp
UNSUBSCRIBE
id:sub-room-10^@
```

#### Validation / Business Rules
- 사용자는 자신이 현재 구독(SUBSCRIBE) 중인 고유 id에 대해서만 구독 취소를 요청할 수 있다.
- 존재하지 않거나 이미 해제된 subscription-id로 취소를 요청할 경우 404 NOT_FOUND 에러가 전파된다.
- 클라이언트는 정상적으로 구독 취소 응답을 수신한 이후부터 해당 채팅방의 브로드캐스트 메시지 수신을 중단한다.
- 웹소켓 연결 세션 자체가 끊어지는 경우(Disconnect)나 차단되는 경우(BANNED)에는 서버가 세션 내부의 모든 구독 정보를 자동으로 취소(Clean up) 처리한다.
---

### 메시지 전송

#### 메시지 전송 Destination

| Destination | Auth | 설명 |
| :--- | :---: | :--- |
| `/app/chat/send/{roomId}` | O | 메시지 전송 요청 (PUB) |

#### Payload 명세
| 필드명 | 타입 | 필수 여부 | 설명 |
| :--- | :---: | :---: | :--- |
| `clientMessageId` | String | **Y** | 클라이언트가 발급한 임시 고유 ID (재전송 시 중복 저장 방지용) |
| `content` | String | **Y** | 메시지 본문 |

#### 메시지 전송 요청 예시 (Client -> Server)

```stomp
SEND
destination:/app/chat/send/201
content-type:application/json

{
  "clientMessageId": "cm-9b2f-1234",
  "content": "방금 홈런!!"
}^@
```

#### 서버 브로드캐스트 응답 예시 (Server -> 해당 방의 전체 구독자)
- Destination: `/topic/chat.room.{roomId}`

```stomp
MESSAGE
subscription-id:sub-room-10
destination:/topic/chat.room.201
content-type:application/json

{
  "messageId": "cm-9b2f-1234",
  "seq": 9982,
  "roomId": 201,
  "senderId": "9",
  "content": "방금 홈런!!",
  "sentAt": "2026-04-27T14:22:30Z"
}^@
```

#### 서버 에러 응답 예시 (Server -> 송신자 본인 1:1 채널)
- Destination: `/user/queue/error`
```stomp
MESSAGE
subscription-id:sub-error-personal
destination:/user/queue/error
content-type:application/json

{
  "clientMessageId": "cm-9b2f-1234",
  "reason": "SAVE_FAILED"
}^@
```

#### Validation / Business Rules
- 인증된 사용자여야 하며, 비공개(1:1 문의) 채팅방의 경우 해당 방의 활성 멤버(참여자)만 메시지를 발송할 수 있습니다. 권한이 없을 시 요청은 거부됩니다.
- clientMessageId가 동일한 요청이 중복으로 들어올 경우, 서버는 이전에 이미 저장된 messageId 정보를 그대로 다시 반환(멱등 처리)하여 메시지 중복 적재를 방지합니다.
- 동일한 채팅방 내부의 메시지는 데이터베이스의 seq (BIGSERIAL 등 자동 증가 PK) 컬럼의 단조 증가 성질을 이용하여 클라이언트 렌더링 시 완벽한 순서를 보장합니다.

---

### 읽음 상태 갱신

#### 읽음 상태 갱신 Destination
| Destination | Auth | 설명 |
| :--- | :---: | :--- |
| `/app/chat/read/{roomId}` | O | 읽음 상태 갱신 요청 (PUB) |

#### Payload 명세
| 필드명 | 타입 | 필수 여부 | 설명 |
| :--- | :---: | :---: | :--- |
| `lastReadMessageId` | Long | **Y** | 사용자가 마지막으로 읽은 메시지 ID |

#### 읽음 상태 갱신 요청 예시 (Client -> Server)
```stomp
SEND
destination:/app/chat/read/201
content-type:application/json

{
  "lastReadMessageId": 9982
}^@
```

#### 서버 브로드캐스트 응답 예시 (Server -> 같은 방의 전체 구독자)
- Destination: `/topic/chat.room.{roomId}.read`
```stomp
MESSAGE
subscription-id:sub-read-10
destination:/topic/chat.room.201.read
content-type:application/json

{
  "roomId": 201,
  "userId": 9,
  "lastReadMessageId": 9982,
  "readAt": "2026-04-27T14:22:35Z"
}^@
```

#### Validation / Business Rules
- 요청 사용자 및 방에 대한 권한 검증을 수행합니다. Redis의 chat:room:{roomId}:user:{userId}:lastRead 값과 비교하여 전달받은 ID가 더 큰 경우에만 단조 증가를 인정하고 갱신합니다.
- 기존 저장된 값보다 작거나 같은 lastReadMessageId 요청은 처리하지 않고 무시합니다.
- 실시간 브로드캐스트는 즉시 수행하되, 고부하를 방지하기 위해 RDB 영속화는 별도 스케줄러(2~3초 주기) 또는 웹소켓 연결 종료(DISCONNECT) 시점에 일괄 반영(Flush)합니다.
- 본 이벤트는 1:1 문의방 등 비공개 방 멤버들 간의 동기화에 유효하며, 대규모 공개 채팅방의 읽음 표시는 시스템 정책에 따라 비활성화될 수 있습니다.

---

### 액세스 토큰 갱신

#### Destination
| Destination | Auth | 설명 |
| :--- | :---: | :--- |
| `/app/auth/refresh` | O | 세션 내 액세스 토큰 갱신 (PUB) |

#### Payload 명세
| 필드명 | 타입 | 필수 여부 | 설명 |
| :--- | :---: | :---: | :--- |
| `token` | String | **Y** | 새로 발급받은 액세스 토큰 |

#### Validation / Business Rules
- 토큰의 subject(userId)가 현재 세션의 principal과 일치해야 한다. 불일치 시 무시된다.
- 검증 실패 시 갱신되지 않으며, grace 시간 초과 후 서버가 세션을 종료한다.

---

### 서버 발신 실시간 이벤트 종류

#### 메시지 이벤트 — `/topic/chat.room.{roomId}`

| 필드 | 타입 | 설명 |
| :--- | :---: | :--- |
| `messageId` | String | 클라이언트 발급 임시 ID (clientMessageId) |
| `seq` | Long | DB 저장 후 발급되는 메시지 PK (정렬 기준) |
| `roomId` | Long | 채팅방 ID |
| `senderId` | String | 발신자 ID |
| `content` | String | 메시지 본문 |
| `sentAt` | Instant | 서버 수신 시각 (UTC) |

#### 읽음 이벤트 — `/topic/chat.room.{roomId}.read`

| 필드 | 타입 | 설명 |
| :--- | :---: | :--- |
| `roomId` | Long | 채팅방 ID |
| `userId` | Long | 읽음 처리한 사용자 ID |
| `lastReadMessageId` | Long | 마지막으로 읽은 메시지 ID |
| `readAt` | Instant | 읽음 처리 시각 (UTC) |
