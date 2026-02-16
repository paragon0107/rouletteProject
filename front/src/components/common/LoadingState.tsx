interface LoadingStateProps {
  label?: string
}

export function LoadingState({ label = '불러오는 중입니다...' }: LoadingStateProps) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white px-4 py-10 text-center text-sm text-slate-600">
      {label}
    </div>
  )
}
