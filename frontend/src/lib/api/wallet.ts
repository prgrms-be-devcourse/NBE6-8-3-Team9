import { apiCall } from './client';
import type { WalletDto, ChargeDto } from '@/lib/types/wallet';

// 지갑 API 호출들
export const walletApi = {
    // 사용자 지갑 전체 조회 (모든 코인 포함)
    getWallet: (userId: number) =>
        apiCall<WalletDto>(`/wallet/users/${userId}`),

    // 충전
    charge: (userId: number, amount: number) => {
        const chargeDto: ChargeDto = { amount };
        return apiCall(`/wallet/users/${userId}/charge`, {
            method: 'POST',
            body: JSON.stringify(chargeDto),
        });
    },
};
