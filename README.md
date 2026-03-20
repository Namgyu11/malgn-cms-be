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

### 요구 사항
- Java 25
- Gradle (wrapper 포함)

### 실행
```bash
./gradlew bootRun
```

애플리케이션 기동 시 `data.sql`에 의해 아래 계정이 자동 생성됩니다.

| username | password  | role  |
|----------|-----------|-------|
| admin    | admin1234 | ADMIN |
| user1    | user11234 | USER  |
| user2    | user21234 | USER  |

- H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:malgn-cms`)
- Swagger UI: http://localhost:8080/swagger-ui.html

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

### 아키텍처: DDD (Domain-Driven Design)

과제에서 별도로 요구한 사항은 아니지만, 레이어 간 책임과 의존성 방향을 명확히 하기 위해 DDD 기반으로 설계하였습니다.

비즈니스 규칙은 도메인 엔티티 내부 메서드로 캡슐화하고, Application Service는 유스케이스 흐름만 담당합니다. Repository는 `domain` 레이어에 인터페이스(Port)로 정의하고 `infrastructure` 레이어에서 Spring Data JPA로 구현(Adapter)하여 의존성 역전 원칙(DIP)을 준수합니다. 이를 통해 도메인 로직이 특정 프레임워크에 종속되지 않고, 테스트 시 Repository를 손쉽게 Mock으로 교체할 수 있습니다.

```
com.springcloud.client.malgncmsbe/
├── contents/                  ← Contents Bounded Context
│   ├── domain/                ← Aggregate Root, Repository Port(인터페이스)
│   ├── application/           ← Use Case (Command/Result 객체)
│   ├── infrastructure/        ← Repository 구현체 (Spring Data JPA)
│   └── interfaces/            ← Controller, Request/Response DTO
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

### 인증 방식: JWT

Session 대신 JWT를 선택한 이유는 REST API의 Stateless 원칙에 부합하기 때문입니다. Session은 서버가 상태를 유지해야 해 수평 확장 시 세션 공유 문제가 발생하는 반면, JWT는 토큰 자체에 `username`과 `role`을 포함하므로 서버 상태 없이 인가 판단이 가능합니다.

```
클라이언트                           서버
   |-- POST /api/auth/login -------->|
   |                                 | 자격증명 검증 → JWT 발급 (HS512, 24시간)
   |<-- { accessToken } -------------|
   |                                 |
   |-- POST /api/contents ---------->| Authorization: Bearer {token}
   |                                 | JwtAuthenticationFilter → SecurityContext 저장
   |<-- 201 Created -----------------|
```

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
| Swagger UI | `/swagger-ui.html` — 인터랙티브 API 문서 및 테스트 |
| 입력값 검증 | `@NotBlank`, `@Size` 등 Bean Validation으로 잘못된 요청 차단 (400 응답) |
| 공통 응답 포맷 | `ApiResponse<T> { success, data, message }` 전 API 통일 |
| 페이징 응답 포맷 | `PageResponse<T> { content, page, size, totalElements, totalPages }` |
| 전역 예외 처리 | `@RestControllerAdvice` 기반, `ErrorCode` enum으로 에러 코드 일원화 |

---

## 기술적 고려사항

### 조회수 동시성: JPQL 벌크 업데이트

상세 조회 시 조회수를 JPA Dirty Checking으로 증가시키면 동시 요청에서 Lost Update가 발생합니다.

```
Thread A: SELECT view_count=5 → +1 → UPDATE 6
Thread B: SELECT view_count=5 → +1 → UPDATE 6  ← 기대값 7, 실제 6
```

이를 방지하기 위해 JPQL 벌크 업데이트로 DB 레벨에서 원자적으로 처리합니다.

```sql
UPDATE contents SET view_count = view_count + 1 WHERE id = :id
```

낙관적 락(`@Version`) 도입도 검토했으나, 조회수 증가는 충돌 시 재시도가 의미 없는 연산이므로 오버엔지니어링이라고 판단하여 제외했습니다.

### `lastModifiedBy` 응답 정합성

JPA Auditing의 `@LastModifiedBy`는 트랜잭션 flush 시점(`@PreUpdate`)에 적용됩니다. flush 전에 응답 DTO를 빌드하면 이전 값이 그대로 반환되는 문제가 있었습니다.

`flush()` + `EntityManager.refresh()` 호출로 해결할 수도 있지만, 서비스 레이어에서 JPA 내부 동작을 직접 제어하는 것은 추상화를 파괴하고 Repository Port에 인프라 관심사가 노출된다는 문제가 있습니다.

대신 `Contents.update(title, description, modifiedBy)` 메서드가 `lastModifiedBy`와 `lastModifiedDate`를 명시적으로 세팅하도록 변경했습니다. 서비스는 이미 인증된 `username`을 알고 있으므로 추가 비용이 없고, 수정 행위에 귀속된 상태는 수정 메서드가 직접 관리한다는 점에서 도메인 모델의 캡슐화에도 부합합니다.

---

## 사용한 AI 도구

본 과제는 **Claude Code (claude-sonnet-4-6)** 를 활용하여 구현하였습니다.

| 단계 | 활용 내용 |
|------|-----------|
| 설계 | 요구사항 분석, 아키텍처 설계, 트레이드오프 검토를 Claude와 함께 진행. 설계 결정은 개발자가 직접 판단 |
| 구현 | `plan.md` 기반으로 Claude Code가 코드 생성 및 테스트 작성 |
| 검증 | curl 기반 API 테스트 시나리오 작성 및 버그 수정 |

> REST API 상세 문서는 [API.md](./API.md)를 참고해주세요.
