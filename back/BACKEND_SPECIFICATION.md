# 백엔드 구현 명세서 (Kotlin + Spring Boot)

## 1. 문서 목적
이 문서는 `BACKEND_REQUIREMENTS.md`와 `API_PROMPTS.md`, 그리고 프로젝트 기본 구현 지침을 통합한 백엔드 구현 기준서입니다.  
목표는 팀이 동일한 기준으로 API, 도메인, 동시성, 예외, 테스트, 배포를 일관되게 구현하도록 하는 것입니다.

## 2. 범위
- 대상: 백엔드(Spring Boot + Kotlin)
- 제외: 프론트엔드 UI/UX 상세 구현
- 우선 구현 DB: `H2`
- 추후 전환 대상 DB: `PostgreSQL`

## 3. 기술 스택
- `Spring Boot 3.x`
- `Kotlin`
- `Java 21`
- `Spring Data JPA`
- `Swagger (springdoc-openapi)`
- 빌드 도구: `Gradle (Kotlin DSL)`

## 4. 아키텍처 및 패키지 구조
도메인 중심 구조를 사용하고 계층 의존 방향은 반드시 `Controller -> Service(UseCase) -> Repository`로 고정합니다.

```text
com.example.project
├── common
│   ├── exception
│   ├── response
│   └── util
├── config
└── domain
    └── {domainName}
        ├── controller
        ├── service        # UseCase 단위
        ├── domain
        ├── repository
        ├── dto
        └── exception
```

## 5. 핵심 도메인 정의
- `User`: 로그인 주체(모킹 로그인 기준 식별자/닉네임)
- `DailyBudget`: 일자별 총 예산/사용 예산/잔여 예산
- `RouletteParticipation`: 사용자의 일자별 룰렛 참여 이력
- `PointLedger`(또는 PointTransaction): 포인트 적립/차감/환불/회수 이력
- `PointBalanceUnit`: 만료일 단위 포인트 잔액(획득일 + 30일)
- `Product`: 주문 가능한 상품
- `Order`: 상품 주문 이력 및 상태

## 6. 기능 요구사항

### 6.1 인증
- 모킹 로그인 제공
- 입력: 닉네임(또는 아이디) 단일 값
- 결과: 사용자 식별 정보(예: userId) 반환

### 6.2 사용자 기능
- 룰렛 참여
  - 1일 1회 제한
  - 포인트 지급 확률
    - 100점: 40%
    - 300점: 30%
    - 500점: 20%
    - 1000점: 10%
- 오늘 룰렛 참여 여부 조회
- 오늘 잔여 예산 조회
- 내 포인트 목록 조회(유효기간 포함)
- 내 포인트 잔액 조회
- 7일 이내 만료 예정 포인트 조회
- 상품 목록 조회
- 상품 주문(포인트 차감)
- 주문 내역 조회

### 6.3 어드민 기능
- 일일 예산 조회/설정
- 참여자 수 조회
- 참여자 목록 조회(참여 취소 가능 식별자 포함)
- 상품 CRUD(목록/등록/수정/삭제)
- 주문 취소(포인트 환불)
- 룰렛 참여 취소(포인트 회수)

## 7. 핵심 비즈니스 규칙
- 일일 총 예산 기본값: `100,000p`
- 예산 소진 시 룰렛 당첨 불가
- 포인트 유효기간: 획득일 + 30일
- 만료된 포인트는 차감 계산에서 제외(사용 불가)
- 주문 시 포인트는 만료 임박 순(오래된 만료일 우선)으로 차감 권장

## 8. 정합성 및 동시성 규칙

### 8.1 중복 참여 방지
- 같은 사용자의 동일 일자 참여는 1회만 성공해야 함
- 권장 구현
  - DB 유니크 제약: `(user_id, participation_date)`
  - 트랜잭션 내 참여 이력 생성 시 중복 예외 처리

### 8.2 예산 초과 지급 방지
- 동시 요청 상황에서도 일일 예산 초과 지급은 절대 금지
- 권장 구현
  - 일일 예산 행에 비관적 락(`PESSIMISTIC_WRITE`) 또는 원자적 업데이트
  - 트랜잭션 내 `잔여 예산 확인 -> 차감 -> 참여/포인트 기록`을 하나의 단위로 처리

### 8.3 취소 정합성
- 주문 취소 시 환불은 실제 차감 이력과 1:1 대응
- 룰렛 참여 취소 시 지급 포인트 회수 이력 생성
- 중복 취소 방지 상태값(예: `CANCELED`) 관리

## 9. API 설계 명세(권장안)
아래는 구현 시 사용할 권장 REST 엔드포인트입니다.

### 9.1 인증
- `POST /api/v1/auth/mock-login`

### 9.2 사용자
- `POST /api/v1/roulette/participations` 룰렛 참여
- `GET /api/v1/roulette/participations/today` 오늘 참여 여부
- `GET /api/v1/budgets/today` 오늘 잔여 예산
- `GET /api/v1/points/me` 내 포인트 목록
- `GET /api/v1/points/me/balance` 내 포인트 잔액
- `GET /api/v1/points/me/expiring?withinDays=7` 만료 예정 포인트
- `GET /api/v1/products` 상품 목록
- `POST /api/v1/orders` 상품 주문
- `GET /api/v1/orders/me` 주문 내역

### 9.3 어드민
- `GET /api/v1/admin/budgets/{date}` 일일 예산 조회
- `PUT /api/v1/admin/budgets/{date}` 일일 예산 설정
- `GET /api/v1/admin/roulette/participants/count?date=yyyy-MM-dd` 참여자 수
- `GET /api/v1/admin/roulette/participants?date=yyyy-MM-dd` 참여자 목록
- `POST /api/v1/admin/roulette/participations/{participationId}/cancel` 참여 취소
- `GET /api/v1/admin/products` 상품 목록
- `POST /api/v1/admin/products` 상품 등록
- `PATCH /api/v1/admin/products/{productId}` 상품 수정
- `DELETE /api/v1/admin/products/{productId}` 상품 삭제
- `POST /api/v1/admin/orders/{orderId}/cancel` 주문 취소

## 10. OpenAPI 작성 기준
`API_PROMPTS.md`의 기준을 한국어 규칙으로 정리하면 다음과 같습니다.

- OpenAPI 기본 버전: `3.1.0` (요청 시에만 3.0.x)
- 필수 섹션: `openapi`, `info`, `servers`, `paths`, `components`
- 공통 스키마 재사용: `$ref` 적극 사용
- `operationId`는 유일해야 함
- 모든 스키마는 `type` 필수 선언
- 날짜/시간은 `format: date-time` 사용
- 각 API는 최소 1개 성공 예시 + 1개 실패 예시 제공
- 상태코드 규칙 준수
  - 200, 201, 204, 400, 401, 403, 404, 409, 422(필요 시), 429, 500, 503

## 11. 표준 에러 응답 규격
모든 에러는 단일 규격 `ApiErrorResponse`를 사용합니다.

```json
{
  "error": {
    "code": "DOMAIN_REASON",
    "message": "에러 메시지",
    "details": [
      {
        "field": "fieldName",
        "reason": "사유"
      }
    ],
    "trace_id": "추적ID"
  }
}
```

대표 에러 코드 예시:
- `AUTH_UNAUTHORIZED`
- `ROULETTE_ALREADY_PARTICIPATED_TODAY`
- `BUDGET_INSUFFICIENT`
- `POINT_BALANCE_INSUFFICIENT`
- `PRODUCT_NOT_FOUND`
- `ORDER_ALREADY_CANCELED`
- `INTERNAL_SERVER_ERROR`

## 12. 코딩 규칙(프로젝트 구현 지침 반영)
- 변수/함수/클래스명은 의도를 명확히 표현
- 축약어 지양, 풀 네이밍 사용
- 함수 단일 책임 유지(권장 10~20줄)
- 3단계 이상 조건 중첩은 Early Return으로 단순화
- 복잡 로직에는 한국어 주석 추가
- 생성자 주입만 사용, 필드 주입 금지
- DTO/Entity 분리(외부 응답에 Entity 노출 금지)
- 상수는 `companion object` 또는 상수 파일로 분리(매직 값 금지)
- `val`/불변 객체 우선, nullable 최소화

## 13. 테스트 기준

### 13.1 단위 테스트
- 확률 보상 계산 로직
- 포인트 만료 계산 로직(30일)
- 주문 차감/환불 계산 로직

### 13.2 통합 테스트
- 룰렛 참여 전체 플로우(로그인 -> 참여 -> 포인트 적립)
- 상품 주문 플로우(차감/주문 이력)
- 어드민 취소 플로우(환불/회수)

### 13.3 동시성 테스트
- 동일 유저 동시 참여 시 1건만 성공
- 다중 유저 동시 참여 시 총 지급이 일일 예산을 초과하지 않음

## 14. 문서화/배포 기준
- Swagger 문서 URL 제공
  - `/swagger-ui/index.html` 또는 `/api-docs`
- CI/CD 필수
  - GitHub Actions 또는 Jenkins
  - 코드 Push 시 빌드/테스트 자동 실행
  - 설정 파일(`.github/workflows/*`) 저장소 포함

## 15. 구현 완료 판단 체크리스트
- [ ] 모킹 로그인 구현
- [ ] 룰렛 1일 1회 + 확률 보상 구현
- [ ] 일일 예산 초과 방지 구현
- [ ] 포인트 유효기간 30일/만료 제외 구현
- [ ] 7일 이내 만료 예정 포인트 조회 구현
- [ ] 상품 CRUD 구현(어드민)
- [ ] 상품 주문/주문 내역 구현
- [ ] 주문 취소/참여 취소 구현(어드민)
- [ ] Swagger 문서화 완료
- [ ] CI/CD 파이프라인 구성 완료
