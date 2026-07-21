# JWKS 기반 WebSocket Handshake 인증 구현 계획

## 배경

현재 `UserHandshakeInterceptor`는 Spring Cloud Gateway가 주입한 `X-User-Id` 헤더를 읽는 방식이다.
변경 후에는 Gateway 없이 auth server의 JWKS endpoint에서 공개키를 가져와 직접 JWT를 검증한다.

---

## 변경 파일 목록

### 1. `build.gradle` — 의존성 추가

```gradle
implementation 'com.nimbusds:nimbus-jose-jwt:9.40'
```

### 2. `JwksPublicKeyProvider` — 신규 생성

**경로**: `infrastructure/auth/JwksPublicKeyProvider.java`

- `@PostConstruct`: 서버 시작 시 JWKS fetch 및 캐싱
- `@Scheduled`: 주기적 키 갱신 (key rotation 대응, 1시간 간격)
- JWT header의 `kid`로 매칭되는 `RSAPublicKey` 반환

```
auth server /.well-known/jwks.json
    └── JWKSet 파싱
    └── kid → RSAPublicKey 맵으로 캐싱
```

### 3. `UserHandshakeInterceptor` — 수정

- `@Component` 전환 (기존: plain class → `new`로 생성)
- `JwksPublicKeyProvider` 주입
- `X-User-Id` 헤더 방식 → query parameter JWT 방식으로 변경

```
ws://host/ws/chat?token=eyJhbGci...
    └── token 추출
    └── JWT header에서 kid 추출
    └── JwksPublicKeyProvider에서 RSAPublicKey 조회
    └── 서명 검증 + 만료 확인
    └── sub claim → userId → attributes 저장
```

### 4. `StompConfig` — 수정

```java
// 변경 전
.addInterceptors(new UserHandshakeInterceptor())

// 변경 후
.addInterceptors(userHandshakeInterceptor)  // @Component 주입
```

### 5. yml 파일 3종 — 설정 추가

```yaml
app:
  auth:
    jwks-uri: ${AUTH_SERVER_JWKS_URI}
```

| 프로필 | 값 |
|--------|----|
| local | `http://localhost:8080/.well-known/jwks.json` |
| dev | `${AUTH_SERVER_JWKS_URI}` |
| prod | `${AUTH_SERVER_JWKS_URI}` |

### 6. `ci-chat-service.yml` — 환경변수 추가

```yaml
AUTH_SERVER_JWKS_URI: http://localhost:8080/.well-known/jwks.json
```

---

## 검증 실패 시 동작

| 상황 | 결과 |
|------|------|
| token 쿼리 파라미터 없음 | handshake 거부 (return false) |
| JWT 서명 불일치 | handshake 거부 |
| JWT 만료 | handshake 거부 |
| kid에 해당하는 키 없음 | handshake 거부 |
| JWKS fetch 실패 (시작 시) | 서버 시작 실패 |

---

## 고려사항

- `sub` claim을 userId로 사용 (auth server 스펙에 따라 조정 필요)
- JWKS fetch에 `RestClient` 사용 (Spring Boot 3.2+)
- key rotation 대응을 위해 캐시 미스 시 즉시 재fetch 로직 추가 권장
