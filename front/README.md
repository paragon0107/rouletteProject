# Point Roulette Web

React + Vite + TypeScript 기반 사용자 웹 프론트엔드입니다.

## 기술 스택
- React
- TypeScript
- Tailwind CSS
- TanStack Query

## 구현 기능
- Mock 로그인 (닉네임 입력)
- 홈(룰렛)
- 확률 비율 반영 룰렛 UI + 회전 애니메이션
- 오늘 참여 여부 조회, 룰렛 참여 API 연동
- 오늘 잔여 예산 주기적 조회 (5% 미만 시 빨간 점멸)
- 내 포인트
- 포인트 목록/만료 상태 표시
- 7일 내 만료 예정 포인트 알림
- 상품 목록
- 내 포인트 기준 구매 가능 여부 표시
- 상품 주문 API 연동
- 주문 내역 조회
- 로딩/에러/빈 상태 처리

## 실행 방법
1. 의존성 설치
```bash
npm install
```

2. 환경 변수 설정 (`.env`)
```bash
VITE_API_BASE_URL=http://localhost:8080
```

3. 개발 서버 실행
```bash
npm run dev
```

## 스크립트
- `npm run dev`: 개발 서버
- `npm run lint`: 린트 검사
- `npm run build`: 프로덕션 빌드
- `npm run preview`: 빌드 결과 미리보기

## 폴더 구조
```text
src
├─ components
│  ├─ common
│  ├─ layout
│  └─ roulette
├─ constants
├─ hooks
├─ lib
├─ pages
├─ services
└─ types
```
