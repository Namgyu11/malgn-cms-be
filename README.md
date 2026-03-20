# Malgn CMS Backend

(주)맑은기술 백엔드 개발자(Java) 코딩 과제 구현물입니다.

---

## 과제 요구사항

### 개발 환경

| 항목 | 내용 |
|------|------|
| Language | Java 25 |
| Framework | Spring Boot 4 |
| Security | Spring Security |
| ORM | JPA |
| Database | H2 |
| 기타 | Lombok (필요 시) |

### 데이터 모델 (Contents)

| 컬럼명 | 설명 | 타입 |
|--------|------|------|
| id | 고유 아이디 | bigint PK not null |
| title | 콘텐츠 제목 | varchar(100) not null |
| description | 콘텐츠 내용 | text |
| view_count | 조회수 | bigint not null |
| created_date | 생성일 | timestamp |
| created_by | 생성자 | varchar(50) not null |
| last_modified_date | 수정일 | timestamp |
| last_modified_by | 수정자 | varchar(50) |

### 구현 기능

- **콘텐츠 CRUD**: 추가 / 목록 조회(페이징 필수) / 상세 조회 / 수정 / 삭제
- **로그인**: Spring Security 기반, Role: ADMIN / USER
- **접근 권한**: 작성자 본인만 수정·삭제 가능, ADMIN은 전체 수정·삭제 가능
- **예외 처리**: 가능한 범위 내에서 구현

---

## 프로젝트 실행 방법

```bash
./gradlew bootRun
```

> Java 25, Gradle wrapper 필요

애플리케이션 기동 시 아래 계정이 자동 생성됩니다.

| username | password  | role  |
|----------|-----------|-------|
| admin    | admin1234 | ADMIN |
| user1    | user11234 | USER  |
| user2    | user21234 | USER  |

- **H2 Console**: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:malgn-cms`)
- **Swagger UI**: http://localhost:8080/swagger-ui.html

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

---

### 아키텍처: DDD (Domain-Driven Design)

#### 기술적 접근 방식

비즈니스 규칙을 도메인 엔티티 내부에 캡슐화하고, 각 레이어의 책임과 의존성 방향을 명확히 하기 위해 DDD 아키텍처를 채택했습니다. Contents와 User를 각각 독립된 Bounded Context로 분리하고, Repository는 `domain` 레이어에 인터페이스(Port)로 정의한 뒤 `infrastructure` 레이어에서 Spring Data JPA로 구현(Adapter)하여 의존성 역전 원칙(DIP)을 준수합니다. Application Service는 유스케이스 흐름만 담당하고, Command/Result 객체를 통해 레이어 간 결합을 끊었습니다.

```
com.springcloud.client.malgncmsbe/
├── contents/                  ← Contents Bounded Context
│   ├── domain/                ← Aggregate Root, Repository Port (인터페이스)
│   ├── application/           ← Use Case, Command / Result 객체
│   ├── infrastructure/        ← Repository 구현체 (Spring Data JPA)
│   └── interfaces/            ← Controller, Request / Response DTO
├── user/                      ← User Bounded Context
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── interfaces/
└── common/                    ← Shared Kernel
    ├── config/                ← SecurityConfig, JpaAuditingConfig
    ├── security/              ← JwtTokenProvider, JwtAuthenticationFilter
    ├── exception/             ← GlobalExceptionHandler, ErrorCode
    └── response/              ← ApiResponse, PageResponse
```

#### 설계 고려사항 및 트레이드오프

| 항목 | DDD Port & Adapter (채택) | Spring Data JPA 직접 사용 |
|------|--------------------------|--------------------------|
| 의존성 방향 | domain → infrastructure (DIP 준수) | domain이 JPA에 직접 의존 |
| 테스트 용이성 | Repository를 Mock으로 교체 가능 | JPA 없이 테스트 어려움 |
| 코드량 | 많음 (인터페이스 + 구현체 분리) | 적음 |
| 레이어 책임 | 명확 | 혼재 가능성 있음 |

코드량이 늘어나는 단점이 있지만, 각 레이어의 책임이 명확해지고 도메인 로직이 프레임워크에 종속되지 않는다는 점에서 DDD 방식을 선택했습니다.

---

### 인증: JWT (JSON Web Token)

#### 기술적 접근 방식

Spring Security의 `UsernamePasswordAuthenticationFilter` 앞에 `JwtAuthenticationFilter`를 추가하여, 매 요청마다 `Authorization: Bearer {token}` 헤더에서 JWT를 파싱하고 `SecurityContextHolder`에 인증 정보를 저장합니다. 토큰 Payload에 `username`과 `role`을 포함시켜 별도의 DB 조회 없이 인가 판단이 가능하도록 했습니다.

```
클라이언트                            서버
   |-- POST /api/auth/login -------->|
   |                                 | 자격증명 검증 → JWT 발급 (HS512, 24시간)
   |<-- { accessToken } -------------|
   |                                 |
   |-- POST /api/contents ---------->| Authorization: Bearer {token}
   |                                 | JwtAuthenticationFilter
   |                                 | → 토큰 파싱 → SecurityContext 저장 → 인가 처리
   |<-- 201 Created -----------------|
```

#### 설계 고려사항 및 트레이드오프

| 항목 | JWT (채택) | Session |
|------|-----------|---------|
| 서버 상태 | Stateless | Stateful (세션 저장소 필요) |
| 수평 확장 | 별도 처리 없이 가능 | 세션 공유 문제 발생 |
| 토큰 무효화 | 만료 전 강제 무효화 어려움 | 즉시 가능 |
| 인가 처리 | 토큰 내 Payload로 판단 | DB/세션 조회 필요 |

토큰 강제 무효화가 어렵다는 단점이 있지만, REST API의 Stateless 원칙에 부합하고 수평 확장이 용이하다는 점에서 JWT를 선택했습니다.

---

### 접근 권한

| 엔드포인트 | 인증 | 권한 |
|-----------|------|------|
| GET /api/contents | 불필요 | — |
| GET /api/contents/{id} | 불필요 | — |
| POST /api/contents | 필요 | USER, ADMIN |
| PUT /api/contents/{id} | 필요 | 작성자 본인, ADMIN |
| DELETE /api/contents/{id} | 필요 | 작성자 본인, ADMIN |

---

## 추가 구현 기능

| 기능 | 설명 |
|------|------|
| 회원가입 API | `POST /api/auth/signup` — USER Role로 신규 계정 생성 |
| 키워드 검색 | `GET /api/contents?keyword=xxx` — title + description 대상 LIKE 검색 |

---

## 기술적 고려사항

### 조회수 동시성: JPQL 벌크 업데이트

상세 조회 시 조회수를 JPA Dirty Checking으로 증가시키면 동시 요청에서 Lost Update가 발생합니다.

```
Thread A: SELECT view_count=5 → +1 → UPDATE 6
Thread B: SELECT view_count=5 → +1 → UPDATE 6  ← 기대값 7, 실제 6
```

JPQL 벌크 업데이트를 사용해 DB 레벨에서 원자적으로 처리하여 이 문제를 해결했습니다.

```sql
UPDATE contents SET view_count = view_count + 1 WHERE id = :id
```

낙관적 락(`@Version`) 도입도 검토했으나, 조회수 증가는 충돌 발생 시 재시도 자체가 의미 없는 연산입니다. 재시도 로직이 필요한 낙관적 락보다 벌크 업데이트가 더 적합하다고 판단하여 채택했습니다.

---

## 사용한 AI 도구

**Claude Code (claude-sonnet-4-6)** 를 개발 보조 도구로 활용하였습니다. 아키텍처 설계, 기술 선택, 트레이드오프 판단, 코드 리뷰는 개발자가 직접 수행했으며, Claude Code는 그 결정을 코드로 옮기는 실행 도구로 사용되었습니다.

### 워크플로우

- **플랜 모드 우선**: 구현 전 설계를 먼저 정리하고 검토한 뒤 실행. 잘못된 방향으로 흘러가는 순간 즉시 중단하고 재설계
- **TDD**: 기능 단위로 테스트를 먼저 작성하고 통과 후 커밋. AI가 만든 변경 사항의 버그를 조기에 발견하기 위한 안전망

### 컨텍스트 관리

- **레이지 로딩**: 세부 스펙은 AI가 필요할 때만 로드하도록 구성하여 토큰 낭비 방지
- **세션 세분화**: 작업 단위로 세션을 분리하여 컨텍스트가 오염되지 않도록 유지
- **자동 메모리**: 세션 간 학습 내용을 유지하여 반복 설명 비용 제거

> REST API 상세 문서는 [API.md](./API.md)를 참고해주세요.
