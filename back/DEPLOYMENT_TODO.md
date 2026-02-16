# 배포 및 운영 점검 체크리스트

## 1. Render 배포 준비
- Render 서비스 생성(Web Service)
- 빌드/실행 명령 설정 확인 (`./gradlew clean build`, `./gradlew bootRun` 또는 jar 실행)
- 환경 변수 등록
  - `SPRING_PROFILES_ACTIVE=postgres`
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`
- 배포 후 헬스체크 엔드포인트 응답 확인

## 2. Neon PostgreSQL 연결
- Neon 프로젝트/DB 생성
- 연결 정보 확보(Host, Port, DB, User, Password, SSL 옵션)
- Render 환경 변수에 Neon 연결 문자열 반영
- 애플리케이션 기동 로그에서 PostgreSQL 연결 성공 확인

## 3. 테이블 제약조건 확인
- `roulette_participations` 기존 전체 유니크 제약 제거 여부 확인
  - `uk_roulette_participations_user_date` 부재 확인
- 활성 참여 기준 유니크 인덱스 존재 확인
  - `uk_roulette_participations_user_date_active`
  - 조건: `WHERE is_canceled = FALSE`
- 검증 시나리오
  - 동일 유저/동일 날짜 + `is_canceled = FALSE` 2건 생성 시 실패
  - 동일 유저/동일 날짜 + `is_canceled = TRUE` 중복 데이터는 허용

## 4. 기능 정상 동작 확인(E2E)
- 인증
  - 사용자 로그인 (`/api/v1/auth/login`)
  - 어드민 로그인 (`/api/v1/auth/admin-login`)
- 룰렛
  - 참여 성공/중복 참여 차단
  - 어드민 취소 후 예산/포인트 회수 정상 반영
- 주문(어드민 포함)
  - 주문 생성
  - 어드민 주문 목록 조회
  - 어드민 주문 상태 변경
  - 어드민 주문 취소 시 포인트 환불/재고 복구
- 포인트/예산/상품 조회 API 응답 정합성 확인

## 5. 최종 점검
- `openapi.yaml`(back, webAdmin) 최신 동기화 상태 확인
- 주요 시나리오 로그/예외 코드(`401/403/404/409`) 확인
- 릴리스 노트에 제약조건 변경 사항 명시
