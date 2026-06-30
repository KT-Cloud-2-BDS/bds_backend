## Member 도메인

### 엔드포인트 목록

| method | path                         | auth required | 설명                    |
|--------|------------------------------|---------------|-----------------------|
| POST   | /api/member/signup           | N             | 플랫폼 자체 회원가입(1)        |
| POST   | /api/member/mail             | N             | 이메일 인증 번호 발송(1-1)     |
| POST   | /api/member/mailCheck        | N             | 이메일 인증 번호 검증(1-2)     |
| POST   | /api/member/social           | N             | 소셜 회원가입 및 로그인(2)      |
| POST   | /api/member/login            | N             | 로그인 및 토큰 발급(3)        |
| POST   | /api/member/token/refresh    | N             | refresh 토큰 재발급(5)     |
| POST   | /api/member/password/verify  | N             | 비밀번호 찾기, 변경 권한 획득(6)  |
| GET    | /api/member/info             | O             | 내 정보(이메일, 닉네임) 조회(7)  |
| PATCH  | /api/member/info             | O             | 내 정보(닉네임) 수정(8)       |
| PATCH  | /api/member/password/reset   | N             | 비밀번호 변경(9)            |
| GET    | /api/member/logout           | O             | 로그아웃(10)              |
| DELETE | /api/member/delete           | O             | 회원 탈퇴(11)             |
| PATCH  | /api/member/role             | O             | 회원 권한 변경(서포터, 메이커)(12) |

### 플랫폼 자체 회원가입

```
POST /api/member/signup
```

Auth Required: **N**

Request Body

| 필드         | 타입     | 필수  | 설명              |
|------------|--------|-----|-----------------|
| `email`    | String | Y   | 가입할 이메일(회원 아이디) |
| `password` | String | Y   | 가입할 비밀번호        |
| `nickname` | String | Y   | 플랫폼에서 사용할 닉네임   |


Response Body

```json
{
  "statusMessage": "회원가입이 완료 되었습니다."
}
```

Validation / Business Rules

- 가입 요청 데이터 중 `email`과 `nickname`의 중복 여부를 확인 후 가입 진행
- 비밀번호는 BCrypt를 통해 암호화한 뒤 DB에 저장
- 최초 회원가입 유저의 권한 기본 값은 `SUPPORTER`로 설정
- 가입 요청 직전, redis에 해당 email의 `Checked` 상태가 존재하는지 검증
- 가입 완료 후 해당 자격 데이터 삭제

---
### 이메일 인증 번호 발송
```
POST /api/member/mail
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
- 생성된 인증 코드는 redis에 `[email : authCode]` 형태로 저장

---

### 이메일 인증 번호 검증
```
POST /api/member/mailCheck
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

### 소셜 회원가입 및 로그인
```
POST /api/member/social
```
Auth Required: **N**

Request Body

| 필드     | 타입     | 필수  | 설명                      |
|--------|--------|-----|-------------------------|
| `code` | String | Y   | 소셜 인증 서버가 발급한 일회성 인증 코드 |

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
- 자체 플랫폼 회원가입 외 소셜 계정을 통해 회원가입 가능
- `code`를 통해 소셜 API 서버에서 이메일, 닉네임을 획득
- `email`이 중복되지 않았다면 자동 회원가입 진행
- 가입 완료, 기존 회원임이 확인되면 JWT 토큰 생성 후 반환
- 자체 플랫폼 가입과 동일하게 최초 가입 시 회원 기본 권한은 `SUPPORTER`

---

### 로그인 및 토큰 발급
```
POST /api/member/login
```

Auth Required: **N**

Request Body

| 필드         | 타입     | 필수  | 설명              |
|------------|--------|-----|-----------------|
| `email`    | String | Y   | 가입한 이메일(회원 아이디) |
| `password` | String | Y   | 비밀번호            |

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
- 회원은 `email`과 `password`를 통해 로그인을 요청
- 회원이 로그인에 실패할 때마다 `email`을 키값으로 실패 횟수를 누적
- 로그인 실패 횟수가 5회 이상인 경우 해당 계정에 대한 로그인 시도를 10분간 강제 차단
- 회원 정보를 조회해 입력된 패스워드와 BCrypt 대조 진행, 불일치 시 예외 발생
- 인증 성공 시 JWT를 생성해 회원에게 반환

---

### refresh 토큰 재발급
```
POST /api/member/token/refresh
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
- 회원의 access token이 만료되면, refresh token을 검증해 새로운 access token 발급
- 회원이 요청 body로 보낸 refresh token 값과 redis에 저장된 값이 동일한지 확인
- 토큰이 만료되었거나 일치하지 않으면 예외 발생
- 검증 성공 시 기존 토큰 삭제 후 새로운 access token, refresh token 발급해 저장

---

### 비밀번호 찾기 및 변경 권한 획득
```
POST /api/member/password/verify
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
- 회원은 비밀번호를 분실했을 때 비밀전호 재설정을 위한 이메일 인증 필요
- 사용자가 입력한 인증번호와 redis 코드 대조
- 일치하면 기존 인증 번호 삭제, 비밀번호를 변경할 수 있는 임시 자격 부여
- 3분의 만료시간 적용

---

### 내 정보 조회
```
GET /api/member/info
```
Auth Required: **O**

Response Body
```json
{
  "email": "bbangdiz@gmail.com",
  "nickname": "빵빠레"
}
```

Validation / Business Rules
- 회원은 자신의 `email`과 `nickname` 조회 가능

---

### 내 정보 수정
```
PATCH /api/member/info
```
Auth Required: **O**

Request Body

| 필드         | 타입     | 필수  | 설명           |
|------------|--------|-----|--------------|
| `nickname` | String | Y   | 변경하고자 하는 닉네임 |

Response Body
```json
{
  "statusMessage": "회원 정보 수정이 완료되었습니다."
}
```

Validation / Business Rules
- nickname은 고유해야 함
- 중복된 nickname으로 변경을 시도할 경우 예외 발생

---

### 비밀번호 변경
```
PATCH /api/member/password/reset
```
Auth Required: **N**

Request Body

| 필드            | 타입     | 필수  | 설명                |
|---------------|--------|-----|-------------------|
| `email`       | String | Y   | 비밀번호를 변경할 이메일     |
| `newPassword` | String | Y   | 변경하고자 하는 새로운 비밀번호 |

Response Body
```json
{
  "statusMessage": "비밀번호 변경이 완료되었습니다."
}
```

Validation / Business Rules
- 인증을 거쳐 자격을 얻은 회원만 비밀번호 재설정 가능
- redis에 비밀번호 변경에 대한 자격이 있는지 확인 후 존재하지 않으면 예외 처리
- 3분의 만료시간 내에서만 비밀번호 재설정 가능
- 기존과 동일한 비밀번호로 재설정 할 수 없음
- 변경 처리 완료 새로운 비밀번호는 BCrypt로 암호화, redis에 부여됐던 자격 삭제

---

### 로그아웃

```
GET /api/member/logout
```

Auth Required: **O**

Response Body
```json
{
  "statusMessage": "로그아웃이 완료되었습니다."
}
```

Validation / Business Rules
- 회원이 로그아웃을 요청할 경우, redis에 refresh token 데이터 삭제
- access token 만료 시간을 계산해 redis에 블랙리스트 등록
- 만료 전까지 해당 토큰을 이용한 모든 접근 거부

---

### 회원 탈퇴
```
DELETE /api/member/delete
```
Auth Required: **O**

Response Body
```json
{
  "statusMessage": "회원 탈퇴가 성공적으로 완료되었습니다."
}
```

Validation / Business Rules
- 회원은 서비스 탈퇴 요청을 할 수 있음
- 탈퇴 시 데이터 보존을 위해 status 값 변경(`ACTIVE` -> `DELETED`)을 통한 soft delete
- 탈퇴 완료 시 redis에 있는 refresh token 삭제

---
### 회원 권한 변경

```
PATCH /api/member/role 
```

Auth Required: **O**

Request Body

| 필드     | 타입     | 필수  | 설명                         |
|--------|--------|-----|----------------------------|
| `role` | String | Y   | 변경할 권한 값(SUPPORTER, MAKER) |

Response Body
```json
{
  "statusMessage": "권한 변경이 완료되었습니다."
}
```

Validation / Business Rules
- 회원은 플랫폼 내에서 `SUPPORTER`권한 또는 `MAKER`권한을 가짐
- 최초 회원가입 시 기본값은 `SUPPORTER`
- 권한 변경을 요청할 경우 유효한 범위내(SUPPORTER, MAKER)에 있다면 추가적인 조건 없이 권한 변경 가능 

---