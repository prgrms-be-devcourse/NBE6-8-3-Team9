package com.back.back9.domain.tradeLog.controller

import com.back.back9.domain.tradeLog.dto.TradeLogRequest
import com.back.back9.domain.tradeLog.dto.TradeLogResponse
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.tradeLog.service.TradeLogService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalTime
import java.util.*

/**
 * 거래내역 컨트롤러
 * 거래내역은 거래가 발생할 때마다 자동으로 생성되며, 사용자가 직접 추가하거나 수정할 수 없음
 * 본인 계정의 거래내역만 조회 가능
 */
@RestController
@RequestMapping("/api/tradeLog")
@Validated
class TradeLogController(
    private val tradeLogService: TradeLogService
) {

    @GetMapping("/wallet/{wallet_id}")
    fun getItems(
        @PathVariable("wallet_id") walletId: Long,
        @ModelAttribute request: TradeLogRequest,
        pageable: Pageable
    ): ResponseEntity<List<TradeLogResponse>> {
        val startDateTime = request.startDate?.atStartOfDay()
        val endDateTime = request.endDate?.atTime(LocalTime.MAX)

        val tradeType: TradeType? = request.type?.let {
            try {
                TradeType.valueOf(it.uppercase(Locale.getDefault()))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid trade type: $it")
            }
        }

        val items = tradeLogService.findByFilter(
            walletId,
            tradeType,
            request.coinId,
            startDateTime,
            endDateTime,
            pageable
        )

        val result = items
            .filterNotNull()          // null 제거
            .map { TradeLogResponse(it) }

        log.info(
            "거래내역 조회 - 지갑 ID: {}, 거래 유형: {}, 코인 ID: {}, 시작일: {}, 종료일: {}, 페이지: {}",
            walletId, request.type, request.coinId, request.startDate, request.endDate, pageable.pageNumber
        )

        return ResponseEntity.ok(result)
    }

    @GetMapping("/user/{user_id}")
    fun getItemsByUserId(
        @PathVariable("user_id") userId: Long,
        @ModelAttribute request: TradeLogRequest,
        pageable: Pageable
    ): ResponseEntity<List<TradeLogResponse>> {
        val startDateTime = request.startDate?.atStartOfDay()
        val endDateTime = request.endDate?.atTime(LocalTime.MAX)

        val tradeType: TradeType? = request.type?.let {
            try {
                TradeType.valueOf(it.uppercase(Locale.getDefault()))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid trade type: $it")
            }
        }

        val items = tradeLogService.findByUserIdAndFilter(
            userId,
            tradeType,
            request.coinId,
            startDateTime,
            endDateTime,
            pageable
        )

        val result = items
            .filterNotNull()
            .map { TradeLogResponse(it) }

        log.info(
            "사용자별 거래내역 조회 - 사용자 ID: {}, 거래 유형: {}, 코인 ID: {}, 시작일: {}, 종료일: {}, 페이지: {}",
            userId, request.type, request.coinId, request.startDate, request.endDate, pageable.pageNumber
        )

        return ResponseEntity.ok(result)
    }

    @PostMapping("/mock")
    fun createMockTradeLogs(): ResponseEntity<String> {
        tradeLogService.createMockLogs()
        return ResponseEntity.ok("Mock trade logs created.")
    }

    companion object {
        private val log = LoggerFactory.getLogger(TradeLogController::class.java)
    }
}
