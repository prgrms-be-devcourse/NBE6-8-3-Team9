package com.back.back9.domain.analytics.dto

import java.math.BigDecimal

// 평가/실현 수익률 응답 클라이언트용
data class ProfitRateResponse(
    val walletId: Long?,
    val coinAnalytics: MutableList<ProfitAnalysisDto?>?,
    val profitRateOnInvestment: BigDecimal?,  // 투자 대비 수익률
    val profitRateOnTotalAssets: BigDecimal? //총 자산 대비 수익률
) 