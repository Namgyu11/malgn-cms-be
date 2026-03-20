# Malgn CMS Backend

Spring Boot 기반 CMS(Content Management System) REST API 과제 구현물입니다.

---

## 실행 방법

### 요구 사항
- Java 25
- Gradle (wrapper 포함)

### 실행

```bash
./gradlew bootRun
```

- 애플리케이션 기동 시 H2 인메모리 DB가 초기화되며 아래 계정이 자동 생성됩니다.

| username | password   | role  |
|----------|------------|-------|
| admin    | admin1234  | ADMIN |
| user1    | user11234  | USER  |
| user2    | user21234  | USER  |

### H2 콘솔
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:malgn-cms`
- Username: `sa` / Password: (없음)

### Swagger UI
- URL: http://localhost:8080/swagger-ui.html

---

## 구현 내용

### 기술 스택
| 항목 | 내용 |
|------|------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.4 |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| ORM | Spring Data JPA + H2 |
| API 문서 | springdoc-openapi (Swagger UI) |
| 빌드 | Gradle |

### 아키텍처
DDD(Domain-Driven Design) 기반으로 두 개의 Bounded Context로 구성됩니다.

```
com.springcloud.client.malgncmsbe/
├── contents/          ← Contents Bounded Context
│   ├── domain/        ← Aggregate Root, Repository Port
│   ├── application/   ← Use Case (Command/Result)
│   ├── infrastructure/← Repository 구현체
│   └── interfaces/    ← Controller, DTO
├── user/              ← User Bounded Context
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── interfaces/
└── common/            ← Shared Kernel
    ├── config/        ← Security, JPA Auditing
    ├── security/      ← JWT, Filter
    ├── exception/     ← 전역 예외 처리
    ├── response/      ← 공통 응답 래퍼
    └── init/          ← 초기 데이터
```

### 인증 방식
JWT(JSON Web Token) 기반 Stateless 인증을 사용합니다.
- 로그인 성공 시 `accessToken` 발급 (만료: 24시간)
- 이후 요청 시 `Authorization: Bearer <token>` 헤더 포함

### 접근 권한
- **콘텐츠 목록/상세 조회**: 인증 불필요
- **콘텐츠 생성**: 로그인 필요 (USER, ADMIN)
- **콘텐츠 수정/삭제**: 작성자 본인 또는 ADMIN만 가능

### 동시성 처리
- 조회수 증가: JPQL 벌크 업데이트(`UPDATE contents SET view_count = view_count + 1`)로 DB 레벨 원자적 처리 → Lost Update 방지

---

## 추가 구현 기능

| 기능 | 설명 |
|------|------|
| 회원가입 API | `POST /api/auth/signup` — USER Role로 신규 계정 생성 |
| 키워드 검색 | `GET /api/contents?keyword=xxx` — title + description LIKE 검색 |
| Swagger UI | `/swagger-ui.html`에서 인터랙티브 API 테스트 가능 |
| JPA Auditing | `created_date`, `last_modified_date`, `created_by`, `last_modified_by` 자동 관리 |
| 입력값 검증 | `@NotBlank`, `@Size` 어노테이션으로 유효성 검사 |

---

## REST API 문서

### 인증

#### 로그인
```
POST /api/auth/login
Content-Type: application/json

{ "username": "user1", "password": "user11234" }

Response 200:
{ "success": true, "data": { "accessToken": "eyJ..." } }
```

#### 회원가입
```
POST /api/auth/signup
Content-Type: application/json

{ "username": "newuser", "password": "password1" }

Response 201:
{ "success": true }
```

---

### 콘텐츠

#### 목록 조회 (페이징 + 검색)
```
GET /api/contents?page=0&size=10&sort=createdDate,desc
GET /api/contents?keyword=검색어&page=0&size=10

Response 200:
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 10,
    "totalElements": 3,
    "totalPages": 1
  }
}
```

#### 상세 조회
```
GET /api/contents/{id}

Response 200:
{ "success": true, "data": { "id": 1, "title": "...", "viewCount": 1, ... } }

Response 404:
{ "success": false, "message": "콘텐츠를 찾을 수 없습니다." }
```

#### 생성
```
POST /api/contents
Authorization: Bearer <token>
Content-Type: application/json

{ "title": "제목", "description": "내용" }

Response 201:
{ "success": true, "data": { "id": 1, "title": "제목", ... } }
```

#### 수정
```
PUT /api/contents/{id}
Authorization: Bearer <token>
Content-Type: application/json

{ "title": "새 제목", "description": "새 내용" }

Response 200: { "success": true, "data": { ... } }
Response 403: { "success": false, "message": "수정/삭제 권한이 없습니다." }
Response 404: { "success": false, "message": "콘텐츠를 찾을 수 없습니다." }
```

#### 삭제
```
DELETE /api/contents/{id}
Authorization: Bearer <token>

Response 200: { "success": true }
Response 403: { "success": false, "message": "수정/삭제 권한이 없습니다." }
Response 404: { "success": false, "message": "콘텐츠를 찾을 수 없습니다." }
```

---

## 공통 에러 응답

| HTTP 상태 | 상황 |
|-----------|------|
| 400 | 입력값 검증 실패 |
| 401 | 인증 실패 (잘못된 자격증명, 유효하지 않은 토큰) |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 중복 username |
| 500 | 서버 내부 오류 |

---

## AI 도구 및 참고 자료

본 과제는 **Claude Code (claude-sonnet-4-6)** AI 도구를 활용하여 구현하였습니다.

- 설계 단계: 요구사항 분석, DDD 아키텍처 설계, 트레이드오프 검토를 Claude와 함께 진행
- 구현 단계: `plan.md` 기반으로 Claude Code가 코드 생성 및 테스트 작성
- 활용 방식: 설계 결정은 개발자가 직접 판단(`plan.md`의 5.5 섹션 검토), Claude는 구현 수행
