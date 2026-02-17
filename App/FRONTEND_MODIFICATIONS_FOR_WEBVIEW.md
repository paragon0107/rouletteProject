# Frontend Modifications for WebView Back Button Integration

This document details the necessary changes to the `front` (React) project to correctly handle the device's back button when running inside a native WebView.

## Problem Context

The `front` project is a Single-Page Application (SPA) where navigation between views (e.g., "룰렛", "내 포인트", "상품 목록") is managed by changing internal React component state (`activePage`) rather than by changing the URL.

The native Android WebView's default back button behavior (`webView.canGoBack()`) relies on the URL history. Since the URL never changes in our SPA, `webView.canGoBack()` will always be `false`. This causes the back button to immediately close the `WebViewActivity` instead of navigating between the internal views of the web app.

## Solution: JavaScript Bridge for Back Press Handling

To address this, we will implement a JavaScript bridge. The web app will expose a function to the native environment, allowing the native app to "ask" the web app if it can navigate backward internally.

### Required Changes

You need to modify the `front/src/App.tsx` file.

#### `front/src/App.tsx`

Add the following `useEffect` hook and `declare global` interface within your `App.tsx` component.

```tsx
// front/src/App.tsx

import { useEffect, useState } from 'react'; // useEffect를 import 해야 합니다.
import { useQueryClient } from '@tanstack/react-query';
import { AppLayout, type AppPage } from './components/layout/AppLayout';
import { useAuthSession } from './hooks/use-auth';
import { LoginPage } from './pages/LoginPage';
import { OrdersPage } from './pages/OrdersPage';
import { PointsPage } from './pages/PointsPage';
import { ProductsPage } from './pages/ProductsPage';
import { RoulettePage } from './pages/RoulettePage';
import type { MockLoginResponse } from './types/api';

function renderPage(page: AppPage, userId: number) {
  switch (page) {
    case 'roulette':
      return <RoulettePage userId={userId} />;
    case 'points':
      return <PointsPage userId={userId} />;
    case 'products':
      return <ProductsPage userId={userId} />;
    case 'orders':
      return <OrdersPage userId={userId} />;
    default:
      return null;
  }
}

function App() {
  const queryClient = useQueryClient();
  const { session, isLoggedIn, saveSession, clearSession } = useAuthSession();
  const [activePage, setActivePage] = useState<AppPage>('roulette'); // 초기 페이지는 'roulette'로 설정

  // --- Start of new code to add ---
  useEffect(() => {
    // `window` 객체에 네이티브 앱이 호출할 수 있는 함수를 등록합니다.
    // 이 함수는 디바이스의 뒤로 가기 버튼이 눌렸을 때 네이티브 앱에 의해 호출됩니다.
    window.handleAppBackPress = () => {
      // 현재 활성화된 페이지가 메인 'roulette' 페이지가 아니라면,
      if (activePage !== 'roulette') {
        // 'roulette' 페이지로 상태를 변경하여 뒤로 이동한 효과를 줍니다.
        setActivePage('roulette');
        // 프론트엔드가 뒤로 가기 이벤트를 성공적으로 처리했음을 네이티브 앱에 알리기 위해 `true`를 반환합니다.
        return true;
      }
      // 현재 페이지가 이미 'roulette' 메인 페이지라면,
      // 프론트엔드에서 더 이상 뒤로 갈 곳이 없음을 네이티브 앱에 알리기 위해 `false`를 반환합니다.
      // 이 경우 네이티브 앱이 웹뷰를 닫는 등의 다음 동작을 수행하게 됩니다.
      return false;
    };

    // 컴포넌트 언마운트 시 `window` 객체에서 함수를 정리하여 메모리 누수를 방지합니다.
    return () => {
      delete window.handleAppBackPress;
    };
  }, [activePage]); // `activePage`가 변경될 때마다 `handleAppBackPress` 함수를 업데이트합니다.
  // --- End of new code to add ---

  function handleLoginSuccess(nextSession: MockLoginResponse) {
    saveSession(nextSession);
    setActivePage('roulette');
  }

  function handleLogout() {
    clearSession();
    queryClient.clear();
    setActivePage('roulette');
  }

  if (!isLoggedIn || !session) {
    return <LoginPage onLoginSuccess={handleLoginSuccess} />;
  }

  return (
    <AppLayout
      nickname={session.nickname}
      activePage={activePage}
      onChangePage={setActivePage}
      onLogout={handleLogout}
    >
      {renderPage(activePage, session.userId)}
    </AppLayout>
  );
}

// `window` 객체에 `handleAppBackPress` 속성이 존재함을 TypeScript에 알립니다.
// 이 코드는 App.tsx 파일의 최하단에 추가되어야 합니다.
declare global {
  interface Window {
    handleAppBackPress?: () => boolean;
  }
}

export default App;
```

### Explanation of Changes

1.  **`useEffect` Hook**:
    *   This hook runs after every render where `activePage` has changed.
    *   It registers a global JavaScript function `window.handleAppBackPress` that the native WebView can call.
2.  **`window.handleAppBackPress` Function**:
    *   When called by the native app, this function checks the current `activePage`.
    *   If `activePage` is *not* `'roulette'` (meaning the user is on a sub-page), it calls `setActivePage('roulette')` to navigate back to the main page and returns `true`. This tells the native app that the back press was handled by the web app.
    *   If `activePage` *is* already `'roulette'` (meaning the user is on the main page with no internal history to go back to), it returns `false`. This signals to the native app that it should proceed with its default back action (e.g., closing the `WebViewActivity`).
3.  **`declare global` Interface**:
    *   This TypeScript specific declaration informs the TypeScript compiler that the `window` object will have an optional `handleAppBackPress` method. This prevents TypeScript errors during compilation.
4.  **Dependency Array `[activePage]`**:
    *   Ensures that the `handleAppBackPress` function is re-registered whenever `activePage` changes, so it always has the most up-to-date page state.

## Next Steps

1.  Apply these changes to your `front/src/App.tsx` file.
2.  **Rebuild and redeploy your `front` web application.** The native app can only interact with these changes once they are live on your deployed URL (`https://roulette-front-psi.vercel.app/`).
3.  Once the frontend is updated and deployed, you will then need to **modify the native Android code** (`WebViewActivity.kt`) to call this JavaScript function, as detailed in the previous step-by-step solution.
