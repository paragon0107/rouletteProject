export function formatPoints(value: number): string {
  return `${value.toLocaleString('ko-KR')}p`
}

export function formatDateTime(value: string): string {
  const date = new Date(value)
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatDate(value: string): string {
  const date = new Date(value)
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}

export function getDaysUntil(targetDateTime: string): number {
  const now = new Date()
  const target = new Date(targetDateTime)
  const diffTime = target.getTime() - now.getTime()
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
}

export function isExpired(targetDateTime: string): boolean {
  return new Date(targetDateTime).getTime() < Date.now()
}

export function getBudgetRemainRatio(totalBudget: number, remainingBudget: number): number {
  if (totalBudget <= 0) {
    return 0
  }

  return remainingBudget / totalBudget
}
