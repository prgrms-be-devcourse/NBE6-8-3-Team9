package com.back.back9.domain.wallet.controller

import com.back.back9.domain.wallet.dto.BuyCoinRequest
import com.back.back9.domain.wallet.dto.ChargePointsRequest
import com.back.back9.domain.wallet.dto.WalletResponse
import com.back.back9.domain.wallet.service.WalletService
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/wallets")
@Validated
class WalletController(
    private val walletService: WalletService
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(WalletController::class.java)
    }

    @GetMapping("/users/{userId}")
    fun getUserWallet(@PathVariable userId: Long): ResponseEntity<WalletResponse> {
        log.info("사용자 지갑 조회 요청 - 사용자 ID: {}", userId)
        return walletService.getUserWallet(userId)
    }

    @PutMapping("/users/{userId}/charge")
    fun chargeWallet(
        @PathVariable userId: Long,
        @Valid @RequestBody request: ChargePointsRequest
    ): ResponseEntity<WalletResponse> {
        log.info("지갑 충전 요청 - 사용자 ID: {}, 충전 금액: {}", userId, request.amount)
        return walletService.chargeWallet(userId, request)
    }

    @PutMapping("/users/{userId}/purchase")
    fun purchaseItem(
        @PathVariable userId: Long,
        @Valid @RequestBody request: BuyCoinRequest
    ): ResponseEntity<WalletResponse> {
        log.info(
            "코인 구매 요청 - 사용자 ID: {}, 구매 금액: {}, 코인 id: {}",
            userId, request.amount, request.coinId
        )
        return walletService.purchaseItem(userId, request)
    }

    @PutMapping("/users/{userId}/sell")
    fun sellItem(
        @PathVariable userId: Long,
        @Valid @RequestBody request: BuyCoinRequest
    ): ResponseEntity<WalletResponse> {
        log.info(
            "코인 판매 요청 - 사용자 ID: {}, 판매 금액: {}, 코인 id: {}",
            userId, request.amount, request.coinId
        )
        return walletService.sellItem(userId, request)
    }
}
