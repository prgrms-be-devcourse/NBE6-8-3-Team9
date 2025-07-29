// 지갑 관련 타입 정의

export interface CoinAmountResponse {
    coinId: number;
    coinSymbol: string;
    coinName: string;
    quantity: number;      // 코인 개수 (예: 0.005개)
    totalAmount: number;   // 총 투자 금액
}

export interface WalletDto {
    walletId: number;      // Long 타입 (JavaScript에서는 number)
    userId: number;        // Long 타입 (JavaScript에서는 number)
    address: string;
    balance: number;       // BigDecimal (JavaScript에서는 number)
    coinAmounts: CoinAmountResponse[];
}

export interface ChargeDto {
    amount: number;
}
