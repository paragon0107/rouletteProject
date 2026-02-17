# rouletteProject

포인트 룰렛 서비스 전체 모노레포(루트)입니다. 실제 실행 단위는 아래 4개 프로젝트입니다.

- `back`: Spring Boot + Kotlin 백엔드 API
- `front`: 사용자용 웹 프론트엔드(React + Vite)
- `webAdmin`: 관리자 웹(React + Vite + Ant Design)
- `app`: Flutter 앱(네이티브 WebView 기반)

## 1) 전체 구조 한눈에 보기

```text
rouletteProject/
├── back/       # API 서버 (서브모듈)
├── front/      # 사용자 웹 (서브모듈)
├── webAdmin/   # 어드민 웹 (서브모듈)
└── app/        # Flutter 앱 (네이티브 WebView 래퍼)
```

루트 `.gitmodules` 기준:
- `back`, `front`, `webAdmin`, `app` 모두 Git 서브모듈

## 2) 4개 프로젝트가 어떻게 같이 동작하는가

```text
[사용자 브라우저(front)]
   └─(X-USER-ID 헤더)→ [back API]

[어드민 브라우저(webAdmin)]
   ├─ POST /api/v1/auth/admin-login (admin 코드)
   └─(X-ADMIN-TOKEN 헤더)→ [back API]

[모바일 앱(app)]
   └─ 네이티브 WebView로 front 웹을 로드
      └─ front가 back API 호출
```

핵심 포인트:
- 실제 비즈니스 상태(예산/룰렛/포인트/주문)는 모두 `back`이 소유합니다.
- `front`, `webAdmin`, `app`은 각각 사용자 UI/관리자 UI/모바일 컨테이너 역할입니다.

## 3) 인증/권한 모델

### 사용자 인증 (Mock Login)
- 엔드포인트: `POST /api/v1/auth/login`
- 입력: 닉네임
- 결과: `userId`, `nickname`, `role`
- 이후 사용자 API 호출 시 `X-USER-ID` 헤더 사용

### 관리자 인증
- 엔드포인트: `POST /api/v1/auth/admin-login`
- 입력: `adminCode`
- **중요: `admin`을 입력하면 로그인 성공**
- 성공 시 백엔드가 아래를 반환:
  - `headerName = X-ADMIN-TOKEN`
  - `adminToken = ADMIN-STATIC-TOKEN`
- 이후 관리자 API 호출 시 `X-ADMIN-TOKEN` 헤더 사용

## 4) 백엔드(`back`) 상세

### 기술 스택
- Spring Boot `3.3.5`
- Kotlin `1.9.25`
- Java `21`
- Spring Data JPA + JDBC
- springdoc OpenAPI (`/swagger-ui/index.html`, `/api-docs`)
- DB: PostgreSQL(기본), H2(local-h2 프로필)

### 도메인
- `User`
- `DailyBudget`
- `RouletteParticipation`
- `PointUnit` (잔액 단위)
- `PointTransaction` (이력)
- `Product`
- `Order`

### 핵심 비즈니스 규칙
- 1일 1회 룰렛 참여
- 룰렛 보상 확률:
  - 100p: 40%
  - 300p: 30%
  - 500p: 20%
  - 1000p: 10%
- 일일 기본 예산: `100,000p`
- 포인트 만료: 적립 시점 + 30일
- 만료 임박: 7일 이내
- 주문 시 포인트 차감은 만료일 빠른 순으로 진행

### 동시성/정합성 장치
- 예산 차감: 조건부 SQL 업데이트(`used + points <= total`)로 초과 지급 방지
- 룰렛 중복 방지:
  - PostgreSQL에서 활성 참여(`is_canceled=false`) 기준 부분 유니크 인덱스
- 취소 정합성:
  - 룰렛 취소 시 보상 포인트 미사용분만 회수 가능
  - 주문 취소는 `PLACED` 상태에서만 허용
- 동시성 시나리오 테스트 파일 존재:
  - `back/src/test/kotlin/com/roulette/backend/concurrency/PointRouletteConcurrencyScenarioTest.kt`

### 사용자/관리자 API 요약

#### 인증
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/admin-login`

#### 사용자 API (`X-USER-ID` 필요)
- `POST /api/v1/roulette/participations`
- `GET /api/v1/roulette/participations/today`
- `GET /api/v1/budgets/today`
- `GET /api/v1/points/me`
- `GET /api/v1/points/me/balance`
- `GET /api/v1/points/me/expiring`
- `GET /api/v1/products`
- `POST /api/v1/orders`
- `GET /api/v1/orders/me`

#### 관리자 API (`X-ADMIN-TOKEN` 필요)
- `GET /api/v1/admin/budgets/{date}`
- `PUT /api/v1/admin/budgets/{date}`
- `GET /api/v1/admin/roulette/participants/count`
- `GET /api/v1/admin/roulette/participants`
- `POST /api/v1/admin/roulette/participations/{participationId}/cancel`
- `GET /api/v1/admin/products`
- `POST /api/v1/admin/products`
- `PATCH /api/v1/admin/products/{productId}`
- `DELETE /api/v1/admin/products/{productId}`
- `GET /api/v1/admin/orders`
- `PATCH /api/v1/admin/orders/{orderId}/status`
- `POST /api/v1/admin/orders/{orderId}/cancel`

### 에러 응답 포맷
모든 에러는 공통 구조를 사용합니다.

```json
{
  "error": {
    "code": "...",
    "message": "...",
    "details": [
      { "field": "...", "reason": "..." }
    ],
    "trace_id": "..."
  }
}
```

## 5) 시드 데이터: `userA ~ userD` (중요)

앱 시작 시 `StartupSeedDataInitializer`가 기본 데이터를 구성합니다.
이미 `userA`가 존재하면 시드 초기화 전체를 건너뜁니다(중복 생성 방지).

### 기본 사용자
- `userA`
- `userB`
- `userC`
- `userD`

### 사용자별 트랜잭션 성격
- `userA`
  - 계정만 생성됨
  - 초기 룰렛/주문 트랜잭션 없음 (빈 상태 테스트용)
- `userB` (가벼운 히스토리)
  - 최근 5일 룰렛 참여 시드
  - 보상 고정 시퀀스: `[100, 300, 500, 300, 1000]`
  - 주문 시도 12회 패턴(일부 취소)
- `userC` (중간~많은 히스토리)
  - 80일치 룰렛 참여 패턴
  - 주문 시도 140회 패턴(일부 취소)
- `userD` (가장 많은 히스토리)
  - 90일치 룰렛 참여 패턴
  - 주문 시도 180회 패턴(일부 취소)

참고:
- 주문은 당시 보유 포인트/재고 조건을 만족해야 실제 생성되므로, “시도 횟수”와 “최종 생성 건수”는 다를 수 있습니다.
- 룰렛/주문 취소에 따라 포인트 환불/회수 이력이 함께 생성됩니다.

### 시드 상품
- `시드_미니커피` (40p)
- `시드_아메리카노` (120p)
- `시드_스낵박스` (350p)
- `시드_프리미엄박스` (900p)

## 6) 사용자 웹(`front`) 상세

### 기술 스택
- React `19`
- TypeScript
- Vite `7`
- Tailwind CSS `4`
- TanStack Query `5`

### 주요 기능
- 닉네임 기반 로그인
- 룰렛 페이지
  - 당첨값 기반 회전 애니메이션
  - 오늘 참여 여부 조회
  - 오늘 예산 10초 주기 조회
  - 잔여 예산 5% 미만 시 빨간 점멸
- 내 포인트 페이지
  - 포인트 유닛 목록/상태(사용 가능/사용/만료/취소)
  - 7일 내 만료 예정 포인트 표시
- 상품 페이지
  - 구매 가능 여부 계산(포인트/재고/수량)
  - 주문 생성
- 주문 내역 페이지

### 인증 세션
- `localStorage` 키: `point-roulette-auth-session`

### WebView 뒤로가기 브리지
- `window.handleAppBackPress` 함수를 노출
- 네이티브 앱이 이를 호출해 SPA 내부 페이지 뒤로가기를 처리

## 7) 관리자 웹(`webAdmin`) 상세

### 기술 스택
- React `18`
- TypeScript
- Vite `7`
- Ant Design `6`

### 로그인
- 로그인 화면에서 인증 코드 입력
- 백엔드 `/api/v1/auth/admin-login` 호출
- **`admin` 입력 시 인증 성공**
- 받은 토큰을 `sessionStorage`(`roulette_web_admin_auth`)에 저장

### 메뉴/기능
- 대시보드
  - 날짜별 예산/참여자 수/지급 포인트 요약
- 예산 관리
  - 일일 예산 조회/수정
  - 룰렛 참여 취소(포인트 회수)
- 상품 관리
  - 상품 목록/등록/수정/삭제
- 주문 내역
  - 주문 목록 조회
  - 주문 상태 `COMPLETED` 처리
  - 주문 취소(포인트 환불)

## 8) 모바일 앱(`app`) 상세

### 기술 스택
- Flutter (Dart)
- Android Kotlin 네이티브 WebView + MethodChannel
- iOS WKWebView + MethodChannel

### 현재 동작 방식
- Flutter 시작 화면에서 바로 네이티브 WebView 실행
- 초기 URL(하드코딩):
  - `https://roulette-front-psi.vercel.app/`
- 즉, 모바일 앱은 사용자 웹(`front`)을 네이티브 WebView에 띄우는 컨테이너 역할

### 주요 구현 포인트
- Android
  - `SplashActivity` -> `MainActivity` -> `WebViewActivity`
  - 로딩 오버레이 / 에러 오버레이 / 재시도 버튼
  - SSL/HTTP/네트워크 에러 처리
  - 뒤로가기 우선순위:
    1. 실제 WebView 히스토리
    2. SPA 브리지(`window.handleAppBackPress`)
    3. 루트에서 2초 내 더블 백으로 앱 종료
- iOS
  - `SceneDelegate`에서 MethodChannel 처리
  - `WKWebView` 네이티브 컨트롤러를 풀스크린 표시
  - 로딩/에러 오버레이, 재시도, 뒤로가기/닫기 버튼 제공

## 9) 로컬 실행 가이드

### 권장 기동 순서
1. `back` 실행
2. `front` 실행
3. `webAdmin` 실행
4. 필요 시 `app` 실행

### 9-1. 백엔드 실행

### 방법 A: H2 메모리 DB (빠른 로컬 테스트)
```bash
cd back
./gradlew bootRun --args='--spring.profiles.active=local-h2'
```

- 서버: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- H2 콘솔: `http://localhost:8080/h2-console`

### 방법 B: PostgreSQL 사용
필수 환경변수:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

```bash
cd back
./gradlew bootRun
```

### 9-2. 사용자 웹 실행
```bash
cd front
npm install
```

`front/.env.local` 예시:
```env
VITE_API_BASE_URL=http://localhost:8080
```

```bash
npm run dev -- --port 5173
```

### 9-3. 어드민 웹 실행
```bash
cd webAdmin
npm install
```

`webAdmin/.env.local` 예시:
```env
VITE_API_BASE_URL=http://localhost:8080
```

```bash
npm run dev -- --port 5174
```

### 9-4. Flutter 앱 실행
```bash
cd app
flutter pub get
flutter run
```

로컬 웹을 앱에 붙이려면 `app/lib/webview_screen.dart`의 `initialUrl`을 개발 URL로 바꿔야 합니다.

## 10) 배포 및 운영

### 확인된 배포 관련 설정
- `front/.env.local` / `webAdmin/.env.local`의 API 기본값:
  - `https://rouletteproject-h652.onrender.com`
- 모바일 WebView 초기 URL:
  - `https://roulette-front-psi.vercel.app/`

### 백엔드 CI/CD
- CI: `back/.github/workflows/ci.yml`
  - `main`, `develop` push / `main` PR에서 테스트 + `bootJar`
- CD: `back/.github/workflows/cd.yml`
  - CI 성공 + `main` 브랜치일 때 Render Deploy Hook 호출

## 11) 문서 사용 시 주의사항

- API의 최종 기준은 실행 중인 백엔드 Swagger(`back`의 `/swagger-ui`)입니다.
- 일부 정적 OpenAPI 파일(`front/openapi.yaml`)은 과거 헤더 정의(`X-ROLE`)가 남아 있을 수 있으므로, 실제 동작은 백엔드 코드와 Swagger를 기준으로 확인하는 것을 권장합니다.

---

필수 요구사항 반영 상태(요청하신 항목):
- `userA~D` 기본 사용자 존재 및 사용자별 다른 시드 트랜잭션 패턴 설명 완료
- 어드민 페이지는 인증코드 `admin` 입력으로 진입 가능하다는 점 명시 완료
- 4개 프로젝트가 어떻게 연결되어 동작하는지 전체 흐름/역할/실행 방법 상세 정리 완료
