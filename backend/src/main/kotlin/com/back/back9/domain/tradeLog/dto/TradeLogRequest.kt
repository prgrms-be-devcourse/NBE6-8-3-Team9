package com.back.back9.domain.tradeLog.dto

import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

// 프론트 요청 DTO
data class TradeLogRequest(
    val type: String?,
    val coinId: Int?,
    val siteId: Int?,
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @param:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val startDate: LocalDate?,
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @param:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val endDate: LocalDate?
)