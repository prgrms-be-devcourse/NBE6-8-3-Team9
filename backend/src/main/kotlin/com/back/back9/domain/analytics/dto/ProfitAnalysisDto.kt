package com.back.back9.domain.analytics.dto

import java.math.BigDecimal

//평가/실현 수익률 DTO 내부 연산용
@JvmRecord
data class ProfitAnalysisDto(
    val coinName: String?,
    val totalQuantity: BigDecimal?,
    val averageBuyPrice: BigDecimal?,
    val realizedProfitRate: BigDecimal?
)
