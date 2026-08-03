# 🥖 빵빠레 팀 컨벤션

| 버전 | 수정사항 | 작성자 |
| --- | --- | --- |
| v1.0 | 팀 빵빠레 컨벤션 생성 | Team |
| v1.1 | 브랜치 전략 수정 | 손하영 |

---

## 📝 Git Convention

### 1. 커밋 메시지 컨벤션

`[커밋 유형]: [커밋 제목]`

1. 커밋 메시지는 한글로 작성
2. 커밋 유형

    | 커밋 유형 | 의미 |
    | --- | --- |
    | `feat` | 새로운 기능 추가 |
    | `fix` | 버그 수정 |
    | `refactor` | 코드 리팩토링 (동작 변화 없음) |
    | `test` | 테스트 코드, 리팩토링 테스트 코드 추가 |
    | `perf` | 성능 개선 |
    | `docs` | 문서 수정 |
    | `chore` | 패키지 매니저 수정, 그 외 기타 수정 ex) .gitignore |
    | `style` | 코드 formatting, 세미콜론 누락, 코드 자체의 변경이 없는 경우 |
    | `design` | CSS 등 사용자 UI 디자인 변경 |
    | `comment` | 필요한 주석 추가 및 변경 |
    | `rename` | 파일 또는 폴더 명을 수정하거나 옮기는 작업만인 경우 |
    | `remove` | 파일을 삭제하는 작업만 수행한 경우 |
    | `!BREAKING CHANGE` | 커다란 API 변경의 경우 |
    | `!HOTFIX` | 급하게 치명적인 버그를 고쳐야 하는 경우 |

3. 제목은 50자 이내, 직관적인 내용으로 작성
4. 세부 항목은 바디로 추가 작성

    ```
    refactor: 오더 스케줄러 리팩토링

    - Bulk update 적용
       - GameSeat, OrderSeat, Order 레포지토리에 bulk query 생성
       - 기존 단일 업데이트 쿼리 삭제

    - DB 커넥션과 스레드를 짧게 점유하기 위한 트랜잭션 분리
       - OrderService 내부로 select & update 로직 이동

    - fixedRate 60초 -> fixedDelay 1초 변경
       - 강제로 쉬게 함으로써 인프라 환경에서 DB점유를 막기 위함

    - TTL 10분 변경(타 서비스 평균 반영)

    - Scheduler 단위 & 통합테스트
    ```

<br>

### 2. Issue

- 이슈 먼저 생성 후 작업 시작 (태스크 관리 목적)
- 제목(영어): `[유형] ~~~`
    - e.g.) `[FEAT]`
- Issue 본문(한국어 사용, 템플릿 자동설정)

    ```
    ## Feature/Bug

    > 추가하려는 기능에 대해 간결하게 설명해주세요

    * 기능 설명
    * 혹은 버그 설명

    ## 작업 상세 내용

    - [ ] TODO
    - [ ] TODO
    - [ ] TODO

    ## 참고할만한 자료(선택)
    ```

<br>

### 3. Pull Request

- **코드 리뷰**: 팀원 1명 이상으로 구성
- **merge**: PR 담당자
- **제목**: `[DOMAIN] Verb task`
    - e.g.) `[TICKET] Implement CRUD`
- PR 본문(한국어 사용, 템플릿 자동설정)

    ```
    # Related Issues

    - close #[이슈]

    ## 작업 리스트

    ## 작업 내용

    ## 참고사항
    ```

<br>

### 4. Branch 전략: Git-Flow

- `main` : 라이브 서버 제품으로 출시되는 브랜치
- `hotfix` : main 브랜치에서 발생한 버그를 수정하는 브랜치
- `develop` : 다음 출시 버전을 대비하여 개발하는 브랜치 (develop → main)
- `feat` : 추가 기능 개발 브랜치 (feature → develop)
- `refactor` : 추가 기능 없는 코드 수정 브랜치 (리팩토링)
- release 브랜치는 사용 X
- **브랜치명**: `{유형}/{도메인}-{기능}#{issueNum}`
    - e.g.) `feat/noti-setup#1`, `refactor/user-apis#12`

<br>

### 5. Pull 전략

- merge 사용 (IDE에 config 설정): `git config pull.rebase false`

---

## 🔩 코드 스타일

### 1. DTO, Record, Enum 사용

- **Enum**: 스포츠, 구단명
- **Record**: 사용을 우선
    - 내부 구현이 없으면 Record로
    - 내부 구현이 필요하면 class로 (JPA 등)
    - 코드리뷰하면서 변경

### 2. 백엔드 파일 구조: Layer Architecture (계층형 아키텍처)

> 📎 관련 이미지 첨부 예정 (`image.png`)

---

## 🧪 테스트 스타일

1. `@Nested`로 테스트 메서드 작성
2. 메서드명은 한국어를 사용하되, 클래스명은 영어를 사용하는 것이 원칙
3. Exception 테스트는 파일을 분리해서 작성
    - `OrderServiceUnitTest` / `OrderServiceUnitExceptionTest`
    - `OrderServiceIntegrationTest` / `OrderServiceIntegrationExceptionTest`
4. Jacoco 테스트 커버리지: Line 80%, Branch 80% 이상 충족

```java
@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Nested
    @DisplayName("입금 정상 테스트")
    class DepositTest {

        @Test
        void 은행으로부터_정상적_입금_확인_API_요청() {
            ...
        }
    }
}
```

---

## 👀 코드 리뷰

- 리뷰어는 1명
- merge는 PR을 올린 본인이 진행
- Slack 알림은 PR을 올린 사람이 준비가 되었을 때 발송
- AI 리뷰 도구: 코드래빗 (CodeRabbit)

---

## 📐 Code Convention

### URL 규칙

| 기능 | HTTP 메서드 | URL | Controller 메서드 |
| --- | --- | --- | --- |
| 목록 | GET / POST | `/posts` | `list()` |
| 상세 | GET | `/posts/{id}` | `detail()` |
| 등록 | POST | `/posts` | `create()` |
| 수정 | PATCH | `/posts/{id}` | `edit()` |
| 삭제 | DELETE | `/posts/{id}` | `delete()` |

- 리소스를 식별하고, 행위는 메서드로 분리한다.
- 단, 다른 메서드로 처리하기 애매한 다음과 같은 경우 POST 사용 가능
    - JSON으로 조회 데이터를 넘겨야 하는데 GET 메서드를 사용하기 어려운 경우
    - 이외 애매한 경우 (팀원과 상의 후 사용)

<br>

### 클래스 네이밍

| 유형 | 네이밍 규칙 | 예시 |
| --- | --- | --- |
| Controller | `*Controller` | `PostController`, `MemberController` |
| Service | `*Service` | `PostService`, `MemberService` |
| Repository | `*Repository` | `PostRepository`, `MemberRepository` |
| Entity | 단수형 명사 | `Post`, `Member`, `Comment` |
| DTO | `*Dto`, `*Request*`, `*Response*` | `PostRequestDto`, `MemberResponseDto` |

- 파스칼 케이스 사용
- 필요에 따라 명사 + 동사 + 유형을 조합하여 사용
    - ex) `ChatMessageController`, `ChatMessageCreateDto`

<br>

### 메서드 네이밍

| 기능 | Controller | Service | Repository |
| --- | --- | --- | --- |
| 목록 조회 | `list()` | `getAll()` | `findAll()` |
| 단일 조회 | `detail()` | `getById()` | `findById()` |
| 등록 폼 | `createForm()` | - | - |
| 등록 | `create()` | `create()` | `save()` |
| 수정 폼 | `editForm()` | - | - |
| 수정 | `edit()` | `update()` | `save()` |
| 삭제 | `delete()` | `delete()` | `deleteById()` |

- 카멜 케이스 사용
- 명사, 동사, 명사 + 동사 형식
    - ex) `list()`, `save()`, `editForm()`
- 조건, 행위에 따라 위 컨벤션 조건을 맞춰서 명사와 동사를 혼용하여 작성
    - 본 컨벤션에서 서비스는 `find`가 아닌 `get`을 사용해야 함
    - ex) `findRoomByIdOrThrow()` → `getRoomByIdOrThrow()`

<br>

### RESTful한 API를 만들기 위한 4가지 핵심 규칙

**1. HTTP 메서드로 행위 표현**
URI에 `create`, `delete`, `update` 같은 동사를 넣지 않는다. 행위는 HTTP 메서드가 담당한다.
- ❌ `POST /users/create-user`
- ⭕ `POST /users` (생성)
- ❌ `GET /users/delete?id=1`
- ⭕ `DELETE /users/1` (삭제)

**2. 소문자 및 하이픈(-) 사용**
URI는 대소문자를 구분하지만, 혼란을 막기 위해 무조건 소문자를 사용한다. 띄어쓰기가 필요할 때는 언더바(`_`) 대신 하이픈(`-`)을 쓴다.
- ❌ `GET /userProfiles` 또는 `GET /user_profiles`
- ⭕ `GET /user-profiles`

**3. 파일 확장자 포함 금지**
URI 끝에 `.json`, `.xml` 같은 파일 확장자를 붙이지 않는다. 대신 HTTP 헤더의 `Accept`를 사용한다.
- ❌ `GET /users/123/profile-image.jpg`
- ⭕ `GET /users/123/profile-image`

**4. 계층 관계는 슬래시(/)로 표현**
하위 리소스를 표현할 때는 슬래시를 사용해 자연스러운 계층 구조를 만든다.
- ⭕ `GET /users/123/orders` (123번 사용자의 주문 목록 전체 조회)
- ⭕ `GET /users/123/orders/5` (123번 사용자의 주문 중 5번 주문 상세 조회)
