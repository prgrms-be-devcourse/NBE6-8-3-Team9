import { apiCall } from './client'
import type { ProfitRateResponse } from '@/lib/types/analytics'

export const analyticsApi = {
    // 사용자의 거래 내역 조회
    getUserAnalyticsRealized: (userId: number) =>
        apiCall<ProfitRateResponse[]>(`/api/analytics/wallet/${userId}/realized`),
    getUserAnalyticsUnrealized: (userId: number) =>
        apiCall<ProfitRateResponse[]>(`/api/analytics/wallet/${userId}/unrealized`),


}
