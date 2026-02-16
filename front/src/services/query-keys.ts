export const queryKeys = {
  todayParticipation: ['todayParticipation'] as const,
  todayBudget: ['todayBudget'] as const,
  pointList: ['pointList'] as const,
  pointBalance: ['pointBalance'] as const,
  expiringPoints: (withinDays: number) => ['expiringPoints', withinDays] as const,
  products: ['products'] as const,
  orders: ['orders'] as const,
}
