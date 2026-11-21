# 재고 관리 시스템 API

상품의 입고, 출고, 재고 조회를 제공하는 재고 관리 시스템 MVP입니다.
동시에 들어오는 입출고 요청에서도 재고 수량이 어긋나지 않는 것을 가장 중요한 목표로 두었습니다.

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| 언어, 프레임워크 | Java 21, Spring Boot 4.2, Spring Data JPA, Lombok |
| 데이터베이스 | PostgreSQL 16 |
| 스키마 관리 | Flyway |
| 테스트 | JUnit 5, MockMvc, 임베디드 PostgreSQL (zonky) |
| API 문서 | springdoc-openapi (Swagger UI) |
| 코드 스타일 | Checkstyle (네이버 캠퍼스 핵데이 Java 코딩 컨벤션) |
| CI | GitHub Actions |

## 실행 방법

### 1. 테스트 실행

JDK 21만 있으면 됩니다. 테스트는 임베디드 PostgreSQL을 직접 띄우므로 별도의 DB 설치나 Docker가 필요 없습니다.

```bash
./gradlew test
```

코드 스타일 검사까지 함께 실행하려면 `build`를 사용합니다. 규칙을 위반하면 빌드가 실패합니다.

```bash
./gradlew build
```

### 2. 애플리케이션 실행

PostgreSQL에 계정과 데이터베이스를 준비합니다.

```sql
CREATE ROLE inventory LOGIN PASSWORD 'inventory';
CREATE DATABASE inventory OWNER inventory;
```

애플리케이션을 실행하면 Flyway가 테이블을 자동으로 생성합니다.

```bash
./gradlew bootRun
```

접속 정보는 환경변수로 바꿀 수 있습니다.

| 환경변수 | 기본값 |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/inventory` |
| `DB_USERNAME` | `inventory` |
| `DB_PASSWORD` | `inventory` |

실행 후 Swagger UI는 http://localhost:8080/swagger-ui/index.html 에서 확인할 수 있습니다.

## DB DDL

DDL은 [src/main/resources/db/migration/V1__init.sql](src/main/resources/db/migration/V1__init.sql)에 있습니다.

| 테이블 | 설명 |
|---|---|
| `product` | 상품과 현재 재고 수량. 상품명은 유니크, 수량은 `CHECK (quantity >= 0)` |
| `stock_history` | 입출고 이력. 변경 수량과 변경 직후 재고 수량을 함께 기록 |

JPA는 `ddl-auto: validate`로 두어 테이블을 만들지 않고, 엔티티와 DDL이 어긋나면 기동 시점에 실패합니다.

## API

| 기능 | 메서드와 경로 | 성공 응답 |
|---|---|---|
| 입고 | `POST /api/v1/inbounds` | 200 |
| 출고 | `POST /api/v1/products/{productId}/outbounds` | 200 |
| 재고 조회 | `GET /api/v1/products/{productId}` | 200 |
| 상품 목록 조회 | `GET /api/v1/products?name=&page=&size=&sort=&direction=` | 200 |
| 입출고 이력 조회 | `GET /api/v1/products/{productId}/histories?page=&size=&sort=&direction=` | 200 |

### 페이지와 정렬

목록 조회와 이력 조회는 같은 파라미터를 받습니다.

| 파라미터 | 허용 값 | 기본값 |
|---|---|---|
| `page` | 0 이상 | 0 |
| `size` | 1~100 | 20 |
| `sort` | 상품 목록: `id`, `name`, `quantity` / 이력: `id`, `quantity` | `id` |
| `direction` | `asc`, `desc` | 상품 목록: `asc` / 이력: `desc` |

`id`가 아닌 기준으로 정렬할 때는 값이 같은 행의 순서가 페이지마다 달라지지 않도록 `id` 오름차순을 2차 정렬로 붙입니다.

### 입고

등록되지 않은 상품명이면 상품을 새로 등록한 뒤 입고 처리합니다.

```bash
curl -X POST localhost:8080/api/v1/inbounds \
  -H 'Content-Type: application/json' \
  -d '{"name": "상품 A", "quantity": 10}'
```

```json
{"id": 1, "name": "상품 A", "quantity": 10}
```

### 출고

```bash
curl -X POST localhost:8080/api/v1/products/1/outbounds \
  -H 'Content-Type: application/json' \
  -d '{"quantity": 4}'
```

```json
{"id": 1, "name": "상품 A", "quantity": 6}
```

### 재고 조회

```bash
curl localhost:8080/api/v1/products/1
```

```json
{"id": 1, "name": "상품 A", "quantity": 6}
```

### 입출고 이력 조회

```bash
curl localhost:8080/api/v1/products/1/histories
```

```json
{
  "content": [
    {"id": 2, "type": "OUTBOUND", "quantity": 4, "quantityAfter": 6, "createdAt": "2026-11-21T19:33:34.493542Z"},
    {"id": 1, "type": "INBOUND", "quantity": 10, "quantityAfter": 10, "createdAt": "2026-11-21T19:33:34.440919Z"}
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

### 에러 응답

모든 에러는 같은 형식으로 반환합니다.

```json
{
  "code": "INVALID_REQUEST",
  "message": "요청 값이 올바르지 않습니다.",
  "errors": [
    {"field": "quantity", "reason": "수량은 1 이상이어야 합니다."}
  ]
}
```

| HTTP 상태 | code | 상황 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 필수 값 누락, 0 이하 수량, 소수 수량, 잘못된 JSON, 숫자가 아니거나 0 이하인 상품 ID, 범위를 벗어난 `page`와 `size`, 허용하지 않는 `sort`와 `direction` |
| 404 | `PRODUCT_NOT_FOUND` | 존재하지 않는 상품의 출고, 조회 |
| 409 | `INSUFFICIENT_STOCK` | 재고보다 많은 수량 출고 |
| 409 | `STOCK_LIMIT_EXCEEDED` | 저장 가능한 수량 범위를 넘는 입고 |
| 503 | `LOCK_TIMEOUT` | 3초 안에 잠금을 얻지 못함. 재시도 가능 |
| 500 | `INTERNAL_ERROR` | 예상하지 못한 서버 오류 |

## 동시성 제어

### 선택한 방식: 비관적 행 잠금

입고와 출고 모두 한 트랜잭션 안에서 `SELECT ... FOR UPDATE`로 상품 행을 잠근 뒤 수량을 변경하고 이력을 기록합니다.
같은 상품에 대한 요청은 순서대로 처리되고, 서로 다른 상품에 대한 요청은 서로를 막지 않습니다.

**출고 흐름**

1. 상품 행을 `FOR UPDATE`로 잠급니다.
2. 엔티티에서 재고가 충분한지 검증합니다. 부족하면 409를 반환합니다.
3. 수량을 차감하고 이력을 기록한 뒤 커밋합니다.

**입고 흐름**

1. 상품명에 대한 잠금을 얻습니다.
2. 상품명으로 행을 `FOR UPDATE` 조회하고, 없으면 `Product.of(name)`으로 상품 객체를 만들어 저장합니다.
3. 수량을 증가시키고 이력을 기록한 뒤 커밋합니다.

```java
productRepository.lockName(productName);
Product product = productRepository.findByNameForUpdate(productName)
        .orElseGet(() -> productRepository.save(Product.of(productName)));
stockHistoryRepository.save(product.inbound(quantity));
```

### 신규 상품 동시 입고

등록되지 않은 상품은 잠글 행이 없어서 행 잠금만으로는 중복 등록을 막을 수 없습니다.
두 요청이 동시에 "없음"을 확인하고 각자 저장하면 한쪽이 유니크 제약 위반으로 실패합니다.

이 구간은 PostgreSQL의 advisory lock으로 해결했습니다.
입고 트랜잭션은 시작할 때 `pg_advisory_xact_lock`으로 상품명에 대한 잠금을 먼저 얻습니다.
행이 없어도 이름을 기준으로 잠글 수 있으므로, 같은 상품명의 입고는 "조회 후 없으면 생성"을 한 번에 하나씩만 수행합니다.
잠금은 트랜잭션이 끝날 때 자동으로 풀리며, 상품명 유니크 제약은 최후 방어선으로 남아 있습니다.

입고가 advisory lock에 더해 행 잠금도 잡는 이유는 출고 때문입니다.
출고는 상품 ID로 행을 잠그므로, 입고도 같은 행을 잠가야 입고와 출고가 서로 순서대로 처리됩니다.

### 다른 방식과의 비교

| 방식 | 선택하지 않은 이유 |
|---|---|
| 낙관적 락 (`@Version`) | 재고는 같은 상품에 요청이 몰리는 경우가 많아 충돌과 재시도가 급증합니다. |
| 조건부 UPDATE 한 문장 | 가장 빠르지만 도메인 규칙이 SQL에 들어가고, 이력에 남길 변경 후 수량을 얻으려면 구현이 복잡해집니다. |
| Redis 분산 락 | 단일 DB 구성에서는 DB 잠금만으로 충분하며, 운영 요소가 하나 늘어납니다. |

### 추가 방어선

- `CHECK (quantity >= 0)`: 애플리케이션 로직이 잘못되더라도 DB가 음수 재고를 거부합니다.
- `lock_timeout = 3s`: 행 잠금과 advisory lock 모두에 적용되며, 잠금 대기가 길어지면 커넥션을 붙잡지 않고 503을 반환합니다.

### 검증

[InventoryConcurrencyTest](src/test/java/com/inventory/product/InventoryConcurrencyTest.java)에서 실제 PostgreSQL을 대상으로 다음을 확인합니다.

| 시나리오 | 기대 결과 |
|---|---|
| 등록되지 않은 상품에 동시 입고 100건 | 상품 1개, 수량 100, 이력 100건 |
| 재고 50개에 동시 출고 100건 | 50건 성공, 50건 재고 부족, 최종 재고 0 |
| 입고 100건과 출고 100건 동시 요청 | 최종 수량과 이력 합계 일치 |

잠금 대기 초과 시 503을 반환하는 동작은 [InventoryApiTest](src/test/java/com/inventory/product/InventoryApiTest.java)에서 다른 트랜잭션이 행을 잠근 상태를 만들어 확인합니다.

## 설계 결정과 가정

- **상품은 이름으로 식별합니다.** 과제의 상품 구조에 식별 가능한 값이 `name`뿐이라, 입고 요청은 상품명을 받고 상품명에 유니크 제약을 두었습니다. 앞뒤 공백은 제거하고 이름 사이의 연속된 공백은 하나로 줄여서 저장하므로 `상품  A`와 `상품 A`는 같은 상품입니다. 공백 유무(`상품A`와 `상품 A`)와 대소문자는 구분합니다.
- **출고와 조회는 ID로 요청합니다.** 입고는 등록되지 않은 상품도 받아야 하므로 상품명으로 요청하지만, 이미 존재하는 상품을 다루는 출고와 조회는 변하지 않는 값인 `id`로 대상을 지정합니다. `id`는 입고 응답으로 받으며, 상품명으로 찾아야 할 때는 목록 조회의 `name` 파라미터를 사용합니다.
- **입출고 이력을 추가했습니다.** 요구사항에는 없지만 재고 수량의 변동 근거를 추적할 수 있어야 한다고 판단했습니다. 수량 변경과 같은 트랜잭션에서 기록합니다.
- **인증과 인가는 범위에서 제외했습니다.** 모든 API는 인증 없이 호출할 수 있으며, 적용 방식은 아래 확장 고려사항에 적었습니다.
- **수량은 정수만 허용합니다.** `1.5` 같은 값은 잘라내지 않고 400으로 거부합니다.

## 코드 스타일

[네이버 캠퍼스 핵데이 Java 코딩 컨벤션](https://naver.github.io/hackday-conventions-java/)을 따르며, Checkstyle로 검사합니다.
규칙 파일은 [config/checkstyle](config/checkstyle)에 있고, 경고가 하나라도 있으면 빌드가 실패하도록 설정했습니다.

| 항목 | 규칙 |
|---|---|
| 들여쓰기 | 탭, 너비 4 |
| 한 줄 길이 | 최대 120자 |
| import 순서 | static, `java.`, `javax.`, `org.`, `net.`, `com.`, 그 외 순서로 그룹을 나누고 그룹 사이에 빈 줄 |
| 파일 끝 | 줄바꿈으로 종료 |

컨벤션 외에 이 프로젝트에서 추가로 지킨 규칙은 다음과 같습니다.

- 분기는 얼리 리턴으로 작성하고 `else`를 쓰지 않습니다.
- 메서드와 생성자의 매개변수에는 `final`을 붙입니다.
- null 검사는 `Optional`로 처리합니다.
- 객체 생성은 `of`, `from` 정적 팩토리 메서드로 합니다.

## 프로젝트 구조

```
src/main/java/com/inventory
├── common
│   ├── error             공통 에러 코드, 에러 응답, 전역 예외 처리
│   ├── validation        검증 메시지 상수
│   └── web               페이지와 정렬 조립, 입력 문자열 정규화
└── product
    ├── api               컨트롤러
    ├── dto               요청, 응답 객체
    ├── service           트랜잭션 경계와 유스케이스
    ├── repository        JPA 리포지토리
    └── domain            Product, StockHistory 엔티티와 재고 규칙
```

## 확장 고려사항

- **인증과 인가**: 이 과제는 재고 처리의 정합성에 집중하기 위해 인증과 인가를 범위에서 제외했습니다. 실제 서비스에서는 다음과 같이 적용합니다.
  - 인증은 API 게이트웨이 또는 Spring Security의 JWT 검증으로 처리하고, 토큰에 테넌트 ID와 역할을 담습니다.
  - 입고와 출고는 재고 관리 역할만 허용하고, 조회는 조회 역할 이상에 허용합니다.
  - 인증되지 않은 요청은 401, 권한이 없는 요청은 403으로 공통 에러 형식에 맞춰 응답합니다.
  - 이력 테이블에 수행자 ID 컬럼을 추가해 누가 입출고했는지 추적합니다.
- **멱등성**: 네트워크 재시도로 같은 입출고가 두 번 처리되지 않도록 요청별 멱등 키를 받아 이력 테이블에 유니크로 저장하는 방식을 고려할 수 있습니다.
- **멀티 테넌트**: `tenant_id` 컬럼을 추가하고 유니크 제약을 `(tenant_id, name)`으로 바꾸면 현재 잠금 전략을 그대로 유지할 수 있습니다.
- **상품 코드**: 실제 물류 환경에서는 이름 대신 SKU 같은 코드로 상품을 식별하는 것이 적합합니다.
- **창고, 로케이션 단위 재고**: 재고 행을 `(상품, 창고)` 단위로 나누면 잠금 범위도 함께 좁아집니다.
