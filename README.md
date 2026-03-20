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
- **로그인**: Spring Security 기반, 방식 자유 선택 (README 명시 필요), Role: ADMIN / USER
- **접근 권한**: 작성자 본인만 수정·삭제 가능, ADMIN은 전체 수정·삭제 가능
- **예외 처리**: 가능한 범위 내에서 구현
- **추가 구현**: 필요하다고 판단되는 기능 자유 추가 가능

---

## 기술 스택
| 항목 | 내용 |
|------|------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.4 |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| ORM | Spring Data JPA + H2 |
| API 문서 | springdoc-openapi (Swagger UI) |
| 빌드 | Gradle |

---

## 아키텍처

DDD(Domain-Driven Design) 기반으로 두 개의 Bounded Context와 Shared Kernel로 구성하였습니다.

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

---

## 기술적 접근 방식

### 인증: JWT (JSON Web Token)

Session 방식 대신 JWT를 선택한 이유는 REST API의 Stateless 원칙에 부합하기 때문입니다.
Session은 서버가 상태를 유지해야 하므로 수평 확장 시 세션 공유 문제가 발생합니다.
JWT는 토큰 자체에 `username`과 `role`을 포함하므로 서버 상태 없이 인가 판단이 가능합니다.

```
클라이언트                          서버
   |-- POST /api/auth/login -------->|
   |                                 | 자격증명 검증 → JWT 발급 (HS512, 24시간)
   |<-- { accessToken } -------------|
   |                                 |
   |-- POST /api/contents ---------->| Authorization: Bearer {token}
   |                                 | JwtAuthenticationFilter → SecurityContext 저장
   |<-- 201 Created -----------------|
```

### DDD: Port & Adapter 패턴 (Repository 분리)

`domain` 레이어에 Repository 인터페이스(Port)를 정의하고,
`infrastructure` 레이어에서 Spring Data JPA로 구현(Adapter)하는 방식을 택했습니다.

이를 통해 `domain`이 JPA에 직접 의존하지 않아 의존성 역전 원칙(DIP)을 준수하며,
테스트 시 Repository를 Mock으로 교체하기 용이합니다.

### Application Layer: Command / Result 객체 분리

Service 메서드의 파라미터를 `interfaces` 레이어의 DTO로 받으면
`application`이 `interfaces`에 의존하는 역방향 의존이 발생합니다.
이를 방지하기 위해 `CreateContentsCommand`, `UpdateContentsCommand`, `ContentsResult` 등
별도의 Command/Result 객체를 두어 레이어 간 결합을 끊었습니다.

### 입력값 검증

Controller의 `@RequestBody` 파라미터에 `@Valid`와 `@NotBlank`, `@Size` 등의
Bean Validation 어노테이션을 적용하여 잘못된 요청을 서비스 진입 전에 차단합니다.
검증 실패 시 `GlobalExceptionHandler`에서 400 응답으로 통일합니다.

---

## 설계 시 고려사항 및 트레이드오프

### 1. 조회수 동시성: JPQL 벌크 업데이트 선택

상세 조회 시 조회수를 증가시키는 과정에서 동시 요청이 몰리면 Lost Update가 발생할 수 있습니다.

```
Thread A: SELECT view_count=5 → +1 → UPDATE 6
Thread B: SELECT view_count=5 → +1 → UPDATE 6  ← 덮어씌워짐, 실제 기대값은 7
```

JPA Dirty Checking 방식 대신 JPQL 벌크 업데이트를 사용하여 DB 레벨에서 원자적으로 처리합니다.

```sql
UPDATE contents SET view_count = view_count + 1 WHERE id = :id
```

**낙관적 락(`@Version`) 검토**: 조회수 증가는 충돌 발생 시 재시도가 의미 없는 연산이므로,
재시도 로직이 필요한 낙관적 락보다 벌크 업데이트가 더 적합하다고 판단했습니다.
낙관적 락은 오버엔지니어링이라고 결론 내렸습니다.

### 2. `lastModifiedBy` 응답 정합성: 명시적 처리 선택

JPA Auditing의 `@LastModifiedBy`는 트랜잭션 flush 시점(`@PreUpdate`)에 적용됩니다.
수정 후 곧바로 DTO를 빌드하면 flush 전이므로 응답에 이전 값이 담기는 문제가 있었습니다.

`flush()` 후 `EntityManager.refresh()`를 호출하는 방법도 있지만,
서비스 레이어에서 JPA 내부 동작을 제어하는 것은 추상화를 파괴하고
Repository Port에 인프라 관심사가 노출된다는 문제가 있습니다.

대신 `Contents.update(title, description, modifiedBy)` 메서드가
`lastModifiedBy`와 `lastModifiedDate`를 직접 세팅하도록 변경했습니다.
서비스는 이미 `username`을 알고 있으므로 추가 비용이 없고,
"수정 행위에 귀속된 상태는 수정 메서드가 직접 관리한다"는 DDD 원칙에도 부합합니다.

### 3. DDD Repository 분리 트레이드오프

| 항목 | Port & Adapter 분리 (채택) | Spring Data JPA 직접 사용 |
|------|--------------------------|--------------------------|
| 의존성 방향 | domain → infrastructure (DIP 준수) | domain이 JPA에 직접 의존 |
| 테스트 용이성 | Repository Mock 교체 용이 | JPA 없으면 테스트 어려움 |
| 코드량 | 많음 (인터페이스 + 구현체) | 적음 |
| DDD 정합성 | 높음 | 낮음 |

과제가 DDD를 요구하므로 코드량이 늘더라도 Port & Adapter 방식을 선택했습니다.

### 4. JPA Auditing 범위 결정

`created_by`, `created_date`는 생성 시 단 한 번 설정되며 `@PrePersist`에서 즉시 반영되므로
JPA Auditing(`@CreatedBy`, `@CreatedDate`)을 그대로 활용합니다.

`last_modified_by`, `last_modified_date`는 위에서 설명한 응답 정합성 문제로
엔티티 메서드에서 명시적으로 처리합니다.

### 5. 초기 데이터: `data.sql` 선택

애플리케이션 기동 시 테스트 계정을 삽입하는 방법으로,
`ApplicationRunner`/`CommandLineRunner`를 이용한 Java 코드 방식 대신
`src/main/resources/data.sql`을 선택했습니다.

Java 코드 방식은 `UserRepository`, `PasswordEncoder` 빈에 의존하여
도메인 레이어가 초기화 관심사와 섞입니다.
`data.sql`은 순수 SQL 파일로, Spring Boot가 스키마 생성 후 자동 실행합니다.
(`spring.jpa.defer-datasource-initialization=true` 설정으로 Hibernate 스키마 생성 이후 실행 보장)

---

## 추가 구현 기능

| 기능 | 설명 |
|------|------|
| 회원가입 API | `POST /api/auth/signup` — 기본 USER Role로 신규 계정 생성 |
| 키워드 검색 | `GET /api/contents?keyword=xxx` — title + description 대상 LIKE 검색 |
| Swagger UI | `/swagger-ui.html` — 인터랙티브 API 문서 및 테스트 |
| 입력값 검증 | `@NotBlank`, `@Size` 등 Bean Validation 적용, 400 응답 |
| 전역 예외 처리 | `@RestControllerAdvice` 기반 `GlobalExceptionHandler`, `ErrorCode` enum |
| 공통 응답 포맷 | `ApiResponse<T> { success, data, message }` 통일 |
| 페이징 응답 포맷 | `PageResponse<T> { content, page, size, totalElements, totalPages }` |

---

## 테스트 계정

애플리케이션 기동 시 `data.sql`에 의해 아래 계정이 자동 생성됩니다.

| username | password  | role  |
|----------|-----------|-------|
| admin    | admin1234 | ADMIN |
| user1    | user11234 | USER  |
| user2    | user21234 | USER  |

- H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:malgn-cms`)
- Swagger UI: http://localhost:8080/swagger-ui.html

---

## AI 도구 활용

본 과제는 **Claude Code (claude-sonnet-4-6)** 를 활용하여 구현하였습니다.

| 단계 | 활용 내용 |
|------|-----------|
| 설계 | 요구사항 분석, DDD 아키텍처 설계, 트레이드오프 검토를 Claude와 함께 진행. 설계 결정은 개발자가 직접 판단 |
| 구현 | `plan.md` 기반으로 Claude Code가 코드 생성 및 테스트 작성 |
| 검증 | curl 기반 API 테스트 시나리오 작성 및 버그 수정 |

> REST API 상세 문서는 [API.md](./API.md)를 참고해주세요.
