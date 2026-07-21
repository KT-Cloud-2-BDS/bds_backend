# Chat 도메인

## 엔드포인트 목록

| method | path                                         | auth required | 설명                           |
|--------|----------------------------------------------|-----------|------------------------------|
| POST   | `/api/chat/Inquiries`                        | O         | 1:1 문의 채팅방 생성                |
| GET    | `/api/chat/Inquiries`                        | O         | 내 참여 문의 채팅방 목록 조회            |
| GET    | `/api/chat/Inquiries/{roomId}`               | O         | 1:1 문의 채팅방 상세 조회             |
| DELETE | `/api/chat/Inquiries/{roomId}/members/me`    | O       | 1:1 문의 채팅방 나가기               |
| DELETE  | `/api/chat/rooms/{roomId}/close`             | O         | 공개 채팅방 삭제                    |
| POST   | `/internal/chat/fundings/{productId}`        | X         | 펀딩 제품 생성시 공개 채팅방 자동 생성 (시스템) |
| GET    | `/api/chat/fundings/{productId}`             | X         | 공개 채팅방 조회                    |
| POST   | `/api/chat/fundings/{roomId}/ban`            | O         | 공개 채팅방 사용자 BAN               |
| DELETE | `/api/chat/fundings/{roomId}/ban/{targetId}` | O   | 공개 채팅방 사용자 BAN 해제            |
| GET    | `/api/chat/rooms/messages`                   | O         | 채팅 이력 조회                     |
| GET    | `/api/chat/Inquiries/{roomId}/messages`      | O         | 1:1 문의 채팅방 메시지 조회            |
| GET    | `/api/chat/fundings/{roomId}/messages`       | O         | 공개 채팅방 메시지 조회                |
| DELETE | `/api/chat/messages/{messageId}`             | O         | 메시지 삭제(soft delete)          |
| WS     | `/ws/chat`                                   | X         | Websocket 연결                 |
| SUB    | `/topic/rooms/{roomId}`                      | O/X       | 채팅방 구독                       |
| UNSUB  | `subscription-id`                            | O/X       | 채팅방 구독 취소                    |
| PUB    | `/app/chat.send`                             | O         | 메시지 전송                       |
| PUB    | `/app/chat.read`                             | O         | 읽음 상태 갱신                     |

---

## 1:1 문의 채팅방 생성

```
POST /api/chat/inquiry
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
GET /api/chat/inquiry
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
GET /api/chat/inquiry/{roomId}
```

Auth Required: **O**

Response Body

```json
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
- 사용자의 상태(ACTIVE, LEFT, BANNED)를 myMembership 필드를 통해 확인할 수 있다.
---

## 1:1 문의 채팅방 나가기

```
DELETE /api/chat/inquiry/{roomId}/members/me
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
PATCH /api/chat/rooms/{roomId}/close
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
POST /internal/chat/funding/{productId}
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
GET /api/chat/funding/{productId}
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
POST /api/chat/funding/{roomId}/ban
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
DELETE /api/chat/funding/{roomId}/ban/{targetId}
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
GET /api/chat/inquiry/{roomId}/messages
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
GET /api/chat/funding/{roomId}/messages
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
WS  /ws/chat   (STOMP over WebSocket, SockJS fallback 지원)
```

#### CONNECT 헤더

| 헤더       | 필수   | 설명     |
|----------|------|--------|
|Authorization | 	N 	 | Bearer {accessToken} |
|accept-version | 	Y 	 |1.1,1.2|
|heart-beat |	N |	10000,10000 |


- 인증 실패 시 STOMP ERROR 프레임 후 연결 종료.
- 동일 사용자의 다중 디바이스 접속 허용 (sessionId 별로 분리 관리).
- 연결 성공 시 /user/queue/system 으로 다음 페이로드를 푸시
```json
{
    "event": "CONNECTED",
    "userId": 9,
    "sessionId": "ws-3f1a..."
}
```

---

### 채팅방 구독

#### 구독 Destination

| 목적지 |  설명     |
|-----|--------|
|/topic/rooms/{roomId}|해당 방의 모든 이벤트 (메시지/퇴장/수정/읽음 등)|


#### SUBSCRIBE 헤더 명세

| 헤더       | 필수   | 설명     |
|----------|------|--------|
| id |	Y 	|subscription id| 
|lastMessageId |	N| 	lastMessageId {MessageId} |

#### 구독 요청 예시 (Client -> Server)
```stomp
SUBSCRIBE
id:sub-room-10
destination:/topic/rooms/10
lastMessageId:532^@
```

#### 구독 성공 응답 예시 (Server -> Client)
```stomp
MESSAGE
subscription-id:<subscriptionId값>
destination:<destination값>
content-type:application/json
content-length:56

{"type":"SUBSCRIBED","destination":"/topic/rooms/201"}^@
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
| id |	Y 	|subscription id| 

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
| `/app/chat.send` | O | 메시지 전송 요청 (PUB) |

#### Payload 명세
| 필드명 | 타입 | 필수 여부 | 설명 |
| :--- | :---: | :---: | :--- |
| `clientMessageId` | String | **Y** | 클라이언트가 발급한 임시 고유 ID (재전송 시 중복 저장 방지용) |
| `roomId` | Long | **Y** | 대상 채팅방 ID |
| `type` | String | **Y** | 메시지 타입 (`TEXT` / `IMAGE` / `FILE`) |
| `content` | String | **Y** | 메시지 본문 또는 업로드된 파일의 결과 URL |

#### 메시지 전송 요청 예시 (Client -> Server)

```stomp
SEND
destination:/app/chat.send
content-type:application/json

{
  "clientMessageId": "cm-9b2f-1234",
  "roomId": 201,
  "type": "TEXT",
  "content": "방금 홈런!!"
}^@
```

#### 서버 브로드캐스트 응답 예시 (Server -> 해당 방의 전체 구독자)
- Destination: `/topic/rooms/{roomId}`

```stomp
MESSAGE
subscription-id:sub-room-10
destination:/topic/rooms/201
content-type:application/json

{
  "event": "MESSAGE_SENT",
  "roomId": 201,
  "occurredAt": "2026-04-27T14:22:30Z",
  "messageId": null,
  "payload": {
    "messageId": 9982,
    "clientMessageId": "cm-9b2f-1234",
    "senderId": 9,
    "type": "TEXT",
    "content": "방금 홈런!!"
  }
}^@
```

#### 서버 에러 응답 예시 (Server -> 송신자 본인 1:1 채널)
- Destination: `/user/queue/system`
```stomp
MESSAGE
subscription-id:sub-system-personal
destination:/user/queue/system
content-type:application/json

{
  "event": "MESSAGE_FAILED",
  "clientMessageId": "cm-9b2f-1234",
  "errorCode": "INVALID_INPUT",
  "message": "MessageContent must be <= 500 characters but 600"
}^@
```

#### Validation / Business Rules
- 인증된 사용자여야 하며, 비공개(1:1 문의) 채팅방의 경우 해당 방의 활성 멤버(참여자)만 메시지를 발송할 수 있습니다. 권한이 없을 시 요청은 거부됩니다.
- clientMessageId가 동일한 요청이 중복으로 들어올 경우, 서버는 이전에 이미 저장된 messageId 정보를 그대로 다시 반환(멱등 처리)하여 메시지 중복 적재를 방지합니다.
- 동일한 채팅방 내부의 메시지는 데이터베이스의 messageId (BIGSERIAL 등 자동 증가 PK) 컬럼의 단조 증가 성질을 이용하여 클라이언트 렌더링 시 완벽한 순서를 보장합니다.

---

### 읽음 상태 갱신

#### 읽음 상태 갱신 Destination
| Destination | Auth | 설명 |
| :--- | :---: | :--- |
| `/app/chat.read` | O | 읽음 상태 갱신 요청 (PUB) |

#### Payload 명세
| 필드명 | 타입 | 필수 여부 | 설명 |
| :--- | :---: | :---: | :--- |
| `roomId` | Long | **Y** | 대상 채팅방 ID |
| `lastReadMessageId` | Long | **Y** | 사용자가 마지막으로 읽은 메시지 ID |

#### 읽음 상태 갱신 요청 예시 (Client -> Server)
```stomp
SEND
destination:/app/chat.send
content-type:application/json

{
  "roomId": 201,
  "lastReadMessageId": 9982
}^@
```

#### 서버 브로드캐스트 응답 예시 (Server -> 같은 방의 다른 멤버)
- Destination: `/topic/rooms/{roomId}`
```stomp
 MESSAGE
subscription-id:sub-room-10
destination:/topic/rooms/201
content-type:application/json

{
  "event": "READ_UPDATED",
  "roomId": 201,
  "messageId": null,
  "occurredAt": "2026-04-27T14:22:35Z",
  "payload" : {
    "memberId": 9,
    "lastReadMessageId": 9982
  }
}^@
```

#### Validation / Business Rules
- 요청 사용자 및 방에 대한 권한 검증을 수행합니다. Redis의 chat:room:{roomId}:user:{userId}:lastRead 값과 비교하여 전달받은 ID가 더 큰 경우에만 단조 증가를 인정하고 갱신합니다.
- 기존 저장된 값보다 작거나 같은 lastReadMessageId 요청은 처리하지 않고 무시합니다.
- 실시간 브로드캐스트는 즉시 수행하되, 고부하를 방지하기 위해 RDB 영속화는 별도 스케줄러(2~3초 주기) 또는 웹소켓 연결 종료(DISCONNECT) 시점에 일괄 반영(Flush)합니다.
- 본 이벤트는 1:1 문의방 등 비공개 방 멤버들 간의 동기화에 유효하며, 대규모 공개 채팅방의 읽음 표시는 시스템 정책에 따라 비활성화될 수 있습니다.

---

### 서버 발신 실시간 이벤트 종류
서버가 /topic/rooms/{roomId} 경로의 구독자들에게 푸시하는 모든 실시간 이벤트 정보와 Payload 핵심 필드 목록입니다. 모든 이벤트는 통일된 공통 Envelope 구조를 따릅니다.

#### 공통 Envelope 구조
```json
{
  "event": "MESSAGE_SENT",
  "roomId": 201,
  "messageId": null,
  "occurredAt": "2026-04-27T14:22:30Z",
  "payload": { 
    "..." : "이벤트별 특화 필드 포맷 적용" 
  }
}
```

#### Payload 명세 (이벤트별 필드 목록)

각 실시간 `event` 타입에 따라 공통 Envelope의 `payload` 객체 내부에 포함되는 상세 필드 명세입니다.

| EVENT | 발생시점 | 페이로드 핵심 필드 |
| :--- | :--- | :--- |
| **`MESSAGE_SENT`** | 새 메시지 발송 성공 시 | `messageId` (Long, 필수) : 서버 발급 고유 메시지 ID<br>`clientMessageId` (String, 필수) : 클라이언트 임시 ID<br>`senderId` (Long, 필수) : 발신자 고유 ID<br>`type` (String, 필수) : 메시지 포맷 구분 (`TEXT`/`IMAGE`/`FILE`)<br>`content` (String, 필수) : 대화 본문 또는 파일 URL |
| **`MESSAGE_DELETED`** | 메시지 삭제(Soft Delete) 성공 시 | `messageId` (Long, 필수) : 삭제 처리된 대상 메시지 ID |
| **`MEMBER_BANNED`** | 특정 사용자를 방출 및 차단(BAN)했을 시 | `memberId` (Long, 필수) : 해당 채팅방에서 방출 및 차단된 유저 ID |
| **`ROOM_DELETED`** | 채팅방이 완전히 삭제되었을 시 | 하위 페이로드 없음 (공통 Envelope의 `roomId`로 식별) |
| **`READ_RECEIPT`** | 특정 멤버가 메시지를 읽어 상태가 갱신될 시 | `memberId` (Long, 필수) : 읽음 상태를 최근에 갱신한 멤버 ID<br>`lastReadMessageId` (Long, 필수) : 유저가 어디까지 읽었는지 기준이 되는 최신 메시지 ID |
| **`TYPING`** | 특정 멤버의 타이핑 상태가 변화했을 시 | `userId` (Long, 필수) : 현재 타이핑을 조작 중인 유저 ID<br>`typing` (Boolean, 필수) : 타이핑 작동 상태 (`true`: 입력 중, `false`: 입력 멈춤) |