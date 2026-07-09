## Member 도메인

### 엔드포인트 목록

| method | path                        | auth required | 설명                     |
|--------|-----------------------------|---------------|------------------------|
| POST   | /api/members/signup         | N             | 플랫폼 자체 회원가입(1)         |
| POST   | /api/members/social         | N             | 소셜 회원가입 및 로그인(2)       |
| POST   | /api/members/login          | N             | 로그인(3)                 |
| GET    | /api/members/info           | O             | 내 정보(이메일, 닉네임) 조회(6)   |
| PATCH  | /api/members/info           | O             | 내 정보(닉네임) 수정(7)        |
| PATCH  | /api/members/password/reset | N             | 비밀번호 변경(8)             |
| DELETE | /api/members/delete         | O             | 회원 탈퇴(10)              |
| PATCH  | /api/members/role           | O             | 회원 권한 변경(서포터, 메이커)(11) |

### 플랫폼 자체 회원가입

```
POST /api/members/signup
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
- 가입 요청 직전, 인증 서버로 해당 이메일의 유효한 인증 상태 `Checked`가 확인된 경우만 최종 가입
- 비밀번호는 BCrypt를 통해 암호화한 뒤 DB 저장
- 최초 가입 유저의 권한 기본값은 `SUPPORTER`, 상태값은 `ACTIVE`로 설정

---

### 소셜 회원가입 및 로그인
```
POST /api/members/social
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
- 인증 서버로부터 전달받은 `oauth_id`와 `provider` 확인
- `email`, `oauth_id` 기반 중복 검증 진행, 이미 가입된 계정은 기존 회원 정보 반환
- 신규 유저인 경우 member 테이블과 oauth_account 테이블 id를 매핑
- 최조 가입 유저의 권한 기본값은 `SUPPORTER`, 상태값은 `ACTIVE`로 설정

---

### 로그인
```
POST /api/members/login
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
- 인증 서버의 로그인 인증 요청에 따라 실행되는 내부 API
- 회원은 `email`과 `password`를 통해 로그인을 요청
- 입력된 `email`을 기반으로 회원 테이블 조회 후 존재하지 않는 경우 예외 반환
- DB에 저장된 password와 입력된 password의 BCrypt 검증을 진행해 일치 여부를 인증 서버로 응답

---

### 내 정보 조회
```
GET /api/members/info
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
- API 게이트웨이가 JWT 토큰을 해석해 HTTP 해더에 실어준 회원 고유 식별자를 가로채 주체 파악
- 식별자 ID를 기반으로 자신의 `email`과 `nickname` 조회 가능

---

### 내 정보 수정
```
PATCH /api/members/info
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
- API 게이트웨이가 헤더로 넘겨준 회원 식별자를 기반으로 유저 조회
- `nickname`은 고유해야 함
- 중복된 `nickname`으로 변경을 시도할 경우 예외 발생

---

### 비밀번호 변경
```
PATCH /api/members/password/reset
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
- 인증 서버로부터 변경 승인 자격을 임시로 검증받은 요청에 한해 최종 password 변경 승인 
- 기존 비밀번호와 동일한 비밀번호로 변경 시도할 경우 예외 처리
- 새로운 password는 BCrypt로 암호화 해 DB 저장

---

### 회원 탈퇴
```
DELETE /api/members/delete
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
- API 게이트웨이가 헤더로 넘겨준 식별자를 기반으로 대상 특정
- 탈퇴 시 데이터 보존을 위해 status 값 변경(`ACTIVE` -> `DELETED`)을 통한 soft delete
- `deleted_at` 컬럼에 현재 시점 적재

---
### 회원 권한 변경

```
PATCH /api/members/role 
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

