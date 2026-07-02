# Git 개발 전략 — Monorepo + Sparse Checkout

## 개요

본 프로젝트는 **Monorepo** 구조를 채택합니다. 모든 서비스가 하나의 레포지토리에 있지만,
**Sparse Checkout**을 통해 각 개발자는 자신이 담당하는 서비스 코드만 로컬에 받아 작업합니다.

```
bds_backend/                  ← 하나의 레포지토리
├── modules/common/           ← 전 서비스 공통 (항상 포함)
├── services/auth-service/    ← 담당자만 받음
├── services/chat-service/    ← 담당자만 받음
├── services/order-service/   ← 담당자만 받음
└── ...
```

---

## 브랜치 전략

```
main      ← 최종 배포 브랜치 (직접 push 금지)
develop   ← 통합 브랜치 (PR을 통해서만 머지)
feature/* ← 기능 개발 브랜치 (develop에서 분기)
```

---

## 최초 세팅 (1회만 진행)

```bash
# 1. 파일 없이 레포 클론
git clone --no-checkout https://github.com/KT-Cloud-2-BDS/bds_backend.git
cd bds_backend

# 2. sparse-checkout 활성화
git sparse-checkout init

# 3. 내 서비스 + 공통 파일 지정 (담당 서비스에 맞게 변경)
git sparse-checkout set services/{담당 서비스} modules/common build.gradle settings.gradle gradlew gradle

# 4. 파일 내려받기
git checkout
```

### 서비스별 sparse-checkout 명령어

| 담당 서비스 | 명령어 |
|---|---|
| auth-service | `git sparse-checkout set services/auth-service modules/common build.gradle settings.gradle gradlew gradle` |
| chat-service | `git sparse-checkout set services/chat-service modules/common build.gradle settings.gradle gradlew gradle` |
| member-service | `git sparse-checkout set services/member-service modules/common build.gradle settings.gradle gradlew gradle` |
| notification-service | `git sparse-checkout set services/notification-service modules/common build.gradle settings.gradle gradlew gradle` |
| order-service | `git sparse-checkout set services/order-service modules/common build.gradle settings.gradle gradlew gradle` |
| payment-service | `git sparse-checkout set services/payment-service modules/common build.gradle settings.gradle gradlew gradle` |

---

## 개발 흐름

### 1. 작업 브랜치 생성

```bash
git checkout develop
git pull origin develop
git checkout -b feature/{기능명}

# 예시
git checkout -b feature/auth-login
```

### 2. 개발 후 커밋

```bash
git add services/{담당 서비스}/
git commit -m "feat: 로그인 기능 구현"
```

### 3. Push 및 PR 생성

```bash
git push origin feature/{기능명}
```

GitHub에서 `feature/*` → `develop` 으로 PR을 생성합니다.
PR이 열리면 해당 서비스의 CI가 자동으로 실행됩니다.

---

## 최신 코드 받기

```bash
git pull origin develop
```

sparse-checkout이 설정된 상태이므로 담당 서비스 + common 범위만 업데이트됩니다.

---

## CI/CD 흐름

```
feature/* 브랜치 push
        ↓
PR → develop 생성
        ↓
CI 자동 실행 (변경된 서비스만)
  - sparse-checkout으로 해당 서비스 + common만 클론
  - ./gradlew :services:{서비스명}:clean :services:{서비스명}:build
  - 빌드 성공 시 JAR 아티팩트 저장
        ↓
CI 통과 후 develop 머지 가능
```

---

## common 모듈 수정 시 주의사항

`modules/common` 변경은 **모든 서비스 빌드에 영향**을 줍니다.
수정 전 팀원에게 공유하고, 변경 후 각 서비스 담당자가 빌드 이상 여부를 확인해야 합니다.

---

## 새 서비스 추가 시

### Spring Boot (Gradle) 서비스
1. `services/{서비스명}/` 디렉토리 생성 및 하위 Spring Boot 구조 작성
2. `build.gradle` 작성 (아래 서비스 build.gradle 작성 규칙 참고)
3. `.github/workflows/ci-{서비스명}.yml` 생성

> `settings.gradle` 수정 불필요 — 디렉토리가 존재하면 자동으로 Gradle 빌드 대상에 포함됩니다.

### Node.js 등 Gradle을 사용하지 않는 서비스
Gradle 멀티모듈 구조에 포함할 수 없으므로 아래와 같이 처리합니다.

1. `services/{서비스명}/` 디렉토리 생성
2. `settings.gradle` 에 **추가하지 않음** (Gradle 빌드 대상에서 제외)
3. `.github/workflows/ci-{서비스명}.yml` 을 Gradle 대신 해당 언어 빌드 도구로 작성

```yaml
# Node.js 서비스 CI 예시
- name: Install dependencies
  working-directory: services/{서비스명}
  run: npm ci

- name: Build and Test
  working-directory: services/{서비스명}
  run: npm test
```

> `modules/common` 은 Java 라이브러리이므로 Node.js 서비스와 공유 불가합니다.

---

## DB / Redis 추가 시 CI yml 변경

서비스별 DB 또는 Redis가 확정되면 해당 서비스의 CI yml 파일에 `services:` 블록을 추가해야 합니다.

### MySQL 추가 예시 (`ci-{서비스명}.yml`)

```yaml
jobs:
  build-and-test:
    runs-on: ubuntu-latest

    services:
      mysql:
        image: mysql:8
        env:
          MYSQL_DATABASE: {db명}
          MYSQL_ROOT_PASSWORD: root
        ports:
          - 3306:3306
        options: >-
          --health-cmd "mysqladmin ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
```

### PostgreSQL 추가 예시

```yaml
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: {db명}
          POSTGRES_USER: {유저명}
          POSTGRES_PASSWORD: {비밀번호}
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
```

### Redis 추가 예시

```yaml
    services:
      redis:
        image: redis:8
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
```

---

## 서비스 build.gradle 작성 규칙

### 자동 제공 항목 (선언 불필요)

아래 항목들은 루트 `build.gradle` 또는 `modules/common`에서 자동 제공되므로
**각 서비스 `build.gradle` 에 중복 선언하지 않습니다.**

| 항목 | 출처 |
|---|---|
| Java 25 (toolchain) | 루트 `build.gradle` |
| Spring Boot BOM (`spring-boot-dependencies:4.1.0`) | 루트 `build.gradle` |
| `spring-boot-starter-test` (JUnit 5, Mockito 포함) | 루트 `build.gradle` |
| `useJUnitPlatform()` (JUnit 5 실행 설정) | 루트 `build.gradle` |
| Lombok (`compileOnly` + `annotationProcessor`) | 루트 `build.gradle` |
| Spring Boot 플러그인 | 루트 `build.gradle` (`services/*`, `platform/*` 자동 적용) |
| `spring-boot-starter-web` | `modules/common` (`api` 전파) |
| `spring-boot-starter-validation` | `modules/common` (`api` 전파) |

### 필수 선언 항목

```groovy
dependencies {
    implementation project(':modules:common')   // 반드시 포함 — web, validation 전파의 전제조건
}
```

> `project(':modules:common')` 이 없으면 `spring-boot-starter-web`, `spring-boot-starter-validation` 이
> 컴파일 classpath에서 사라져 `@RestController` 등 Spring MVC 어노테이션을 사용할 수 없습니다.

### 작성 예시

```groovy
// services/auth-service/build.gradle
dependencies {
    implementation project(':modules:common')   // 필수

    // 이 서비스에만 필요한 의존성만 추가
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly    'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly    'io.jsonwebtoken:jjwt-jackson:0.12.6'
}
```
