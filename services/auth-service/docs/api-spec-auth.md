## Authentication 도메인

### 엔드포인트 목록

| method | path                       | auth required | 설명                |
|--------|----------------------------|---------------|-------------------|
| POST   | /api/auths/mail            | N             | 이메일 인증 번호 발송(1)   |
| POST   | /api/auths/mailCheck       | N             | 이메일 인증 번호 검증(2)   |
| POST   | /api/auths/login           | N             | 로그인 및 토큰 발급(3)    |
| POST   | /api/auths/token/refresh   | N             | refresh 토큰 재발급(5) |
| POST   | /api/auths/password/verify | N             | 비밀번호 변경 권한 획득(6)  |
| GET    | /api/auths/logout          | O             | 로그아웃(7)           |

### 이메일 인증 번호 발송
```
POST /api/auths/mail
```
  Auth Required: **N**

  Request Body

  | 필드         | 타입     | 필수  | 설명              |
  |------------|--------|-----|-----------------|
  | `email`    | String | Y   | 인증번호를 받을 이메일 주소 |


  Response Body
```json
  {
  "statusMessage": "메일이 성공적으로 발송되었습니다."
  }
```

  Validation / Business Rules
- 시스템 내부에서 6자리 랜덤 인증 코드 생성
- 네이버 SMTP 서버를 이용해 해당 이메일로 인증 코드 전송
- 생성된 인증 코드는 redis에 `[email : authCode]` 형태로 저장, TTL: 3분 설정

---

### 이메일 인증 번호 검증
```
POST /api/auths/mailCheck
```
Auth Required: **N**

Request Body

| 필드         | 타입     | 필수  | 설명                   |
|------------|--------|-----|----------------------|
| `email`    | String | Y   | 인증을 진행하는 이메일 주소      |
| `authCode` | String | Y   | 메일을 받아 입력한 6자리 인증 번호 |


Response Body
```json
{
  "statusMessage": "이메일 확인이 완료되었습니다."
}
```

Validation / Business Rules
- 사용자가 입력한 `authCode`와 redis에 저장된 `email` 코드 대조
- 코드가 존재하지 않거나 불일치할 경우 예외 발생
- 일치하면 redis에 존재하는 기존 인증 번호 데이터 삭제 후 `[email : "Checked]` 기록

---

### 로그인
```
POST /api/auths/login/token
```

Request Body

| 필드         | 타입     | 필수  | 설명      |
|------------|--------|-----|---------|
| `email`    | String | Y   | 가입한 이메일 |
| `password` | String | Y   | 비밀번호    |

Response Body
```json
{
  "grantType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5c...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5c...",
  "accessTokenExpiresIn": 1800000
}
```
Validation / Business Rules
- 로그인 요청 시 redis 조회 후 email이 5회 실패로 인한 차단 상태인지 검증
- 차단 기간 내 요청일 경우 예외 발생
- 차단 상태가 아닌 경우 회원 내부 API를 호출해 회원 유무와 암호화 비밀번호 일치 여부를 리턴받음
- `password` 불일치로 인증 실패 시 redis에 실패 횟수 누적(5회 누적 시 TTL: 10분 설정)
- 인증 성공 시 실패 카운트 초기화 후 JWT를 반환

---

### refresh 토큰 재발급
```
POST /api/auths/token/refresh
```
Auth Required: **N**

Request Body

| 필드             | 타입     | 필수  | 설명                                |
|----------------|--------|-----|-----------------------------------|
| `refreshToken` | String | Y   | 회원이 보유하고 있는 만료되지 않은 refresh token |

Response Body
```json
{
  "grantType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5c...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5c...",
  "accessTokenExpiresIn": 1800000
}
```


Validation / Business Rules
- 요청 받은 refresh token을 파싱해 redis에 보관된 `[RT:email]`값과 대조
- 토큰이 만료되었거나 일치하지 않으면 예외 발생
- 검증 성공 시 기존 토큰 삭제 후 새로운 access token, refresh token 발급해 저장(RTR 적용)

---

### 비밀번호 변경 권한 획득
```
POST /api/auths/password/verify
```
Auth Required: **N**

Request Body

| 필드         | 타입     | 필수  | 설명               |
|------------|--------|-----|------------------|
| `email`    | String | Y   | 비밀번호를 찾고자 하는 이메일 |
| `authCode` | String | Y   | 메일로 수신해 입력한 인증번호 |

Response Body
```json
{
  "statusMessage": "메일 인증에 성공했습니다."
}
```

Validation / Business Rules
- 사용자가 입력한 인증번호와 redis 코드 대조
- 일치하면 기존 인증 번호 삭제, 비밀번호를 변경할 수 있는 임시 자격 부여 `[email: "PASSWORD_CHANGE_ALLOWED]`
- TTL: 3분 강제

---

### 로그아웃

```
GET /api/auths/logout
```

Auth Required: **O**

Response Body
```json
{
  "statusMessage": "로그아웃이 완료되었습니다."
}
```

Validation / Business Rules
- 회원이 로그아웃을 요청할 경우, `[RT:email]`토큰 데이터 삭제
- access token 만료 시간을 계산해 redis에 블랙리스트 등록
- 만료 시간 전까지 게이트웨이 접근 차단

---