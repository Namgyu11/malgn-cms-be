# REST API 문서

> Base URL: `http://localhost:8080`
>
> 인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더를 포함해야 합니다.
>
> Swagger UI: http://localhost:8080/swagger-ui.html

---

## 공통 응답 포맷

```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| success | boolean | 요청 성공 여부 |
| data | Object | 응답 데이터 (실패 시 null) |
| message | String | 오류 메시지 (성공 시 null) |

---

## 공통 에러 코드

| HTTP 상태 | 발생 상황 |
|-----------|-----------|
| 400 | 입력값 검증 실패 (`@NotBlank`, `@Size` 위반) |
| 401 | 인증 실패 (잘못된 자격증명, 만료·위변조된 토큰) |
| 403 | 권한 없음 (미인증 요청, 타인 콘텐츠 수정·삭제 시도) |
| 404 | 리소스 없음 (존재하지 않는 콘텐츠 또는 사용자) |
| 409 | 중복 username으로 회원가입 시도 |
| 500 | 서버 내부 오류 |

---

## 인증 API

### 로그인

```
POST /api/auth/login
Content-Type: application/json
```

**Request Body**
```json
{
  "username": "admin",
  "password": "admin1234"
}
```

**Response 200**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9..."
  },
  "message": null
}
```

**Response 400** — 유효성 실패
```json
{
  "success": false,
  "data": null,
  "message": "아이디를 입력해주세요."
}
```

**Response 401** — 자격증명 불일치
```json
{
  "success": false,
  "data": null,
  "message": "아이디 또는 비밀번호가 올바르지 않습니다."
}
```

---

### 회원가입

```
POST /api/auth/signup
Content-Type: application/json
```

**Request Body**
```json
{
  "username": "newuser",
  "password": "pass1234"
}
```

| 필드 | 제약 |
|------|------|
| username | 필수, 공백 불가 |
| password | 필수, 최소 8자 이상 |

**Response 201**
```json
{
  "success": true,
  "data": null,
  "message": null
}
```

**Response 400** — 유효성 실패
```json
{
  "success": false,
  "data": null,
  "message": "비밀번호는 8자 이상이어야 합니다."
}
```

**Response 409** — 중복 username
```json
{
  "success": false,
  "data": null,
  "message": "이미 사용 중인 아이디입니다."
}
```

---

## 콘텐츠 API

### 목록 조회 (페이징 + 키워드 검색)

```
GET /api/contents
GET /api/contents?keyword=검색어&page=0&size=10&sort=createdDate,desc
```

**Query Parameters**
| 파라미터 | 필수 | 기본값 | 설명 |
|----------|------|--------|------|
| keyword | 선택 | — | title, description 대상 LIKE 검색 |
| page | 선택 | 0 | 페이지 번호 (0부터 시작) |
| size | 선택 | 10 | 페이지당 항목 수 |
| sort | 선택 | createdDate,desc | 정렬 기준 |

**Response 200**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "제목",
        "description": "내용",
        "viewCount": 3,
        "createdDate": "2026-03-20T10:00:00",
        "createdBy": "user1",
        "lastModifiedDate": "2026-03-20T11:00:00",
        "lastModifiedBy": "admin"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  },
  "message": null
}
```

---

### 상세 조회

```
GET /api/contents/{id}
```

호출할 때마다 `view_count`가 1 증가합니다.

**Response 200**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "제목",
    "description": "내용",
    "viewCount": 1,
    "createdDate": "2026-03-20T10:00:00",
    "createdBy": "user1",
    "lastModifiedDate": null,
    "lastModifiedBy": null
  },
  "message": null
}
```

**Response 404**
```json
{
  "success": false,
  "data": null,
  "message": "콘텐츠를 찾을 수 없습니다."
}
```

---

### 콘텐츠 생성

```
POST /api/contents
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**
```json
{
  "title": "콘텐츠 제목",
  "description": "콘텐츠 내용"
}
```

| 필드 | 제약 |
|------|------|
| title | 필수, 공백 불가, 최대 100자 |
| description | 선택 |

**Response 201**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "콘텐츠 제목",
    "description": "콘텐츠 내용",
    "viewCount": 0,
    "createdDate": "2026-03-20T10:00:00",
    "createdBy": "user1",
    "lastModifiedDate": null,
    "lastModifiedBy": null
  },
  "message": null
}
```

**Response 400** — 유효성 실패
```json
{
  "success": false,
  "data": null,
  "message": "제목을 입력해주세요."
}
```

**Response 403** — 미인증 요청
```json
{
  "success": false,
  "data": null,
  "message": "접근이 거부되었습니다."
}
```

---

### 콘텐츠 수정

```
PUT /api/contents/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

작성자 본인 또는 ADMIN만 수정 가능합니다.

**Request Body**
```json
{
  "title": "수정된 제목",
  "description": "수정된 내용"
}
```

**Response 200**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "수정된 제목",
    "description": "수정된 내용",
    "viewCount": 3,
    "createdDate": "2026-03-20T10:00:00",
    "createdBy": "user1",
    "lastModifiedDate": "2026-03-20T12:00:00",
    "lastModifiedBy": "admin"
  },
  "message": null
}
```

**Response 403** — 권한 없음
```json
{
  "success": false,
  "data": null,
  "message": "수정/삭제 권한이 없습니다."
}
```

**Response 404** — 존재하지 않는 콘텐츠
```json
{
  "success": false,
  "data": null,
  "message": "콘텐츠를 찾을 수 없습니다."
}
```

---

### 콘텐츠 삭제

```
DELETE /api/contents/{id}
Authorization: Bearer {accessToken}
```

작성자 본인 또는 ADMIN만 삭제 가능합니다.

**Response 200**
```json
{
  "success": true,
  "data": null,
  "message": null
}
```

**Response 403** — 권한 없음
```json
{
  "success": false,
  "data": null,
  "message": "수정/삭제 권한이 없습니다."
}
```

**Response 404** — 존재하지 않는 콘텐츠
```json
{
  "success": false,
  "data": null,
  "message": "콘텐츠를 찾을 수 없습니다."
}
```
