export function formatNumber(value: number): string {
  return new Intl.NumberFormat('ko-KR').format(value)
}

export function formatPoint(value: number): string {
  return `${formatNumber(value)}p`
}
