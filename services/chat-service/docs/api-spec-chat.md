## Chat 도메인

### 엔드포인트 목록

| method | path                                    | auth required | 설명                              |
|--------|-----------------------------------------|---------------|---------------------------------|
| POST   | /api/chat/rooms                         | O             | 채팅방 생성 (5-1)                    |
| GET    | /api/chat/rooms                         | O             | 내 채팅방 목록 조회 (5-2)               |
| GET    | /api/chat/rooms/game/{gameId}           | N             | 게임별 채팅방 조회 (5-3)                |
| GET    | /api/chat/rooms/{roomId}                | O/N           | 채팅방 상세 조회 (5-4)                 |
| PATCH  | /api/chat/rooms/{roomId}                | O             | 채팅방 정보 수정 name/imageUrl (5-5)   |
| DELETE | /api/chat/rooms/{roomId}                | O             | 채팅방 삭제 (5-6, 방장만 가능)            |
| PATCH  | /api/chat/rooms/{roomId}/archive        | O             | 채팅방 아카이브 (5-7)                  |
| PATCH  | /api/chat/rooms/{roomId}/unarchive      | O             | 채팅방 아카이브 복원 (5-8)               |
| POST   | /api/chat/rooms/{roomId}/join           | O             | 채팅방 입장 (5-9)                    |
| DELETE | /api/chat/rooms/{roomId}/leave          | O             | 채팅방 나가기 (5-10)                  |
| POST   | /api/chat/rooms/{roomId}/invite         | O             | 참여자 초대 (5-11)                   |
| PATCH  | /api/chat/rooms/{roomId}/notification   | O             | 채팅방 알림 설정 변경 (5-12)             |
| POST   | /api/chat/rooms/{roomId}/ban            | O             | 멤버 BAN (5-13)                   |
| GET    | /api/chat/rooms/getMyInvites            | O             | 내 초대 목록 조회 (5-14)               |
| POST   | /api/chat/rooms/{roomId}/reject         | O             | 초대 거부 (5-15)                    |
| GET    | /api/chat/messages/history/{roomId}     | O             | 채팅 이력 조회 — 내가 있을 때 메시지 (5-16)   |
| WS     | /ws/chat                                | O/N           | websocket 연결(5-17-1)            |
| SUB    | /topic/rooms/{roomId}                   | O/N           | 구독 (5-17-2-1)                   |
| UNSUB  | id:subscription-id                      | O/N           | 구독 취소 (5-17-2-2)                |
| PUB    | SEND  /app/chat.send                    | O             | 메시지 전송 (5-17-3-1)               |
| PUB    | SEND  /app/chat.read                    | O             | 읽음 상태 갱신 (5-17-3-2)             |
| PUB    | SEND  /app/chat.typing                  | O             | 타이핑 인디케이터 (5-17-3-3)            |
| DELETE | /api/chat/messages/{messageId}          | O             | 메시지 삭제 soft delete (5-18)       |
| GET    | /api/chat/messages/getMessages/{roomId} | O             | 채팅방 메시지 조회 + lastRead 갱신 (5-19) |

---

### 채팅방 생성

```
POST /api/chat/rooms
```

Auth Required: **O**

Request Body

| 필드           | 타입      | 필수  | 설명                                                 |
|--------------|---------|-----|----------------------------------------------------|
| `type`       | String  | Y   | 채팅방 유형 (`GAME` \| `DIRECT`)                        |
| `name`       | String* | Y/N | 채팅방 표시 이름. `DIRECT` 의 경우 자동 생성되어 생략 가능. `GAME`은 필수 |
| `imageUrl`   | String* | N   | 채팅방 프로필 이미지 URL                                    |
| `gameId`     | Long*   | Y/N | `type=GAME` 일 때 필수                                 |
| `inviteeIds` | Long[]* | Y/N | `DM` / 비공개 방 생성 시 함께 초대할 사용자 ID 목록                 |

Response Body

```json
{
  "success": true,
  "data": {
    "roomId": 201,
    "type": "GAME",
    "gameId": 101,
    "name": "KIA vs 삼성 경기 채팅",
    "imageUrl": null,
    "createdBy": 9,
    "createdAt": "2026-04-21T09:00:00Z"
  }
}
```

Validation / Business Rules

- 인증된 사용자만 생성 가능.
- 동일 사용자 간 `DM` 중복 생성 시 기존 방을 반환 (`DUPLICATE_DIRECT_ROOM` 대신 200 OK + 기존 방).
- `roomId` 는 서버에서 발급, 클라이언트가 보낸 값은 무시.
- `type=GAME` 인 경우 `gameId` 가 존재 여부를 외부 컨텍스트 호출로 검증.

---
