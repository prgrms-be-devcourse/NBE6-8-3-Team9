package com.back.back9.domain.wallet.service

import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.tradeLog.entity.TradeLog
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.tradeLog.repository.TradeLogRepository
import com.back.back9.domain.user.repository.UserRepository
import com.back.back9.domain.wallet.controller.WalletController
import com.back.back9.domain.wallet.dto.*
import com.back.back9.domain.wallet.entity.CoinAmount
import com.back.back9.domain.wallet.entity.Wallet
import com.back.back9.domain.wallet.repository.CoinAmountRepository
import com.back.back9.domain.wallet.repository.WalletRepository
import com.back.back9.global.error.ErrorCode
import com.back.back9.global.error.ErrorException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val coinAmountRepository: CoinAmountRepository,
    private val coinRepository: CoinRepository,
    private val userRepository: UserRepository,
    private val tradeLogRepository: TradeLogRepository
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(WalletController::class.java)
    }

    @Transactional
    fun createWallet(userId: Long): Wallet {
        // Optional 체크 대신 existsByUserId 사용
        if (walletRepository.existsByUserId(userId)) {
            throw ErrorException(ErrorCode.WALLET_ALREADY_EXISTS, userId)
        }

        val user = userRepository.findByIdOrNull(userId)
            ?: throw ErrorException(ErrorCode.USER_NOT_FOUND, userId)

        val wallet = Wallet.builder()
            .user(user)
            .address("Wallet_address_$userId")
            .balance(Money.of(500_000_000L))
            .build()

        walletRepository.save(wallet)
        log.info("새 지갑 생성 완료 - 사용자 ID: {}, 주소: {}", userId, wallet.address)
        return wallet
    }

    @Transactional(readOnly = true)
    fun getUserWallet(userId: Long): ResponseEntity<WalletResponse> {
        val wallet = walletRepository.findByUserId(userId)
            ?: throw ErrorException(ErrorCode.WALLET_NOT_FOUND, userId)

        val validCoinAmounts = wallet.coinAmounts.filter(::isValidCoinAmount)
        log.info(
            "사용자 지갑 조회 완료 - 사용자 ID: {}, 전체 코인: {}개, 유효한 코인: {}개",
            userId, wallet.coinAmounts.size, validCoinAmounts.size
        )
        return ResponseEntity.ok(WalletResponse.fromWithValidCoinAmounts(wallet, validCoinAmounts))
    }

    @Transactional
    fun chargeWallet(userId: Long, request: ChargePointsRequest): ResponseEntity<WalletResponse> {
        val wallet = walletRepository.findByUserId(userId)
            ?: throw ErrorException(ErrorCode.WALLET_NOT_FOUND, userId)

        val chargeAmount = Money.of(request.amount)
        if (!chargeAmount.isGreaterThanZero()) {
            throw ErrorException(ErrorCode.INVALID_REQUEST, "충전 금액은 0보다 커야 합니다.")
        }

        wallet.charge(chargeAmount)
        walletRepository.save(wallet)

        val chargeLog = TradeLog.builder()
            .wallet(wallet)
            .type(TradeType.CHARGE)
            .quantity(BigDecimal.ONE)
            .price(chargeAmount).build()
        tradeLogRepository.save(chargeLog)

        val validCoinAmounts = wallet.coinAmounts.filter(::isValidCoinAmount)
        return ResponseEntity.ok(WalletResponse.fromWithValidCoinAmounts(wallet, validCoinAmounts))
    }

    fun getCoinHoldingsForProfitCalculation(walletId: Long) =
        (walletRepository.findByIdOrNull(walletId)
            ?: throw ErrorException(ErrorCode.WALLET_NOT_FOUND, walletId))
            .coinAmounts.filter(::isValidCoinAmount)
            .map(CoinHoldingInfo::from)

    fun getCoinHoldingsByUserId(userId: Long): List<CoinHoldingInfo> {
        val wallet = walletRepository.findByUserId(userId)
            ?: throw ErrorException(ErrorCode.WALLET_NOT_FOUND, userId)
        return getCoinHoldingsForProfitCalculation(wallet.id)
    }

    private fun isValidCoinAmount(coinAmount: CoinAmount?): Boolean {
        if (coinAmount == null) throw ErrorException(ErrorCode.INVALID_COIN_DATA, "null")
        if (coinAmount.coin == null) throw ErrorException(ErrorCode.INVALID_COIN_DATA, coinAmount.id)
        if (coinAmount.coin.id <= 0) throw ErrorException(ErrorCode.INVALID_COIN_DATA, coinAmount.id)
        if (coinAmount.coin.symbol.isNullOrBlank())
            throw ErrorException(ErrorCode.INVALID_COIN_DATA, coinAmount.id)
        if (coinAmount.totalAmount == null) throw ErrorException(ErrorCode.INVALID_COIN_DATA, coinAmount.id)
        if (coinAmount.quantity == null) throw ErrorException(ErrorCode.INVALID_COIN_DATA, coinAmount.id)
        if (!coinAmount.totalAmount.isGreaterThanZero() && coinAmount.totalAmount != Money.zero()) return false
        if (coinAmount.quantity < BigDecimal.ZERO)
            throw ErrorException(ErrorCode.INVALID_COIN_DATA, coinAmount.id)
        return true
    }

    @Transactional
    fun processTransaction(userId: Long, request: BuyCoinRequest, transactionType: TransactionType): ResponseEntity<WalletResponse> {
        log.info(request.coinId.toString())

        val coin = coinRepository.findByIdOrNull(request.coinId)
            ?: throw ErrorException(ErrorCode.COIN_NOT_FOUND, request.coinId)
        val wallet = walletRepository.findByIdOrNull(request.walletId)
            ?: throw ErrorException(ErrorCode.WALLET_NOT_FOUND, request.walletId)
        userRepository.findByIdOrNull(userId)
            ?: throw ErrorException(ErrorCode.USER_NOT_FOUND, userId)

        if (wallet.user.id != userId) {
            throw ErrorException(ErrorCode.UNAUTHORIZED, "지갑에 대한 접근 권한이 없습니다.")
        }

        val txAmount = Money.of(request.amount)
        if (!txAmount.isGreaterThanZero()) {
            throw ErrorException(ErrorCode.INVALID_REQUEST, "거래 금액은 0보다 커야 합니다.")
        }

        if (transactionType == TransactionType.BUY) {
            if (!wallet.balance.isGreaterThanOrEqual(txAmount)) {
                throw ErrorException(ErrorCode.INSUFFICIENT_BALANCE, "잔액이 부족합니다.")
            }
            wallet.deduct(txAmount)
        } else {
            wallet.charge(txAmount)
        }
        walletRepository.save(wallet)

        var validCoinAmounts = wallet.coinAmounts
            .filter(::isValidCoinAmount)
            .filter { it.coin.id == coin.id }

        if (validCoinAmounts.isEmpty() && transactionType == TransactionType.BUY) {
            val newCoinAmount = CoinAmount.builder()
                .coin(coin).wallet(wallet)
                .quantity(BigDecimal.ZERO)
                .totalAmount(Money.of(BigDecimal.ZERO))
                .build()
            coinAmountRepository.save(newCoinAmount)
            wallet.coinAmounts.add(newCoinAmount)
            validCoinAmounts = listOf(newCoinAmount)
        } else if (validCoinAmounts.isEmpty()) {
            throw ErrorException(ErrorCode.INSUFFICIENT_BALANCE, "판매할 코인이 없습니다.")
        }

        val target = validCoinAmounts[0]
        if (transactionType == TransactionType.BUY) {
            target.addQuantityAndAmount(request.quantity, txAmount)
        } else {
            if (target.quantity < request.quantity) {
                throw ErrorException(ErrorCode.INSUFFICIENT_BALANCE, "보유 수량이 부족합니다.")
            }
            target.subtractQuantityAndAmount(request.quantity, txAmount)
        }
        coinAmountRepository.save(target)

        val list = wallet.coinAmounts
        for (i in 0 until list.size) {
            if (list[i].id == target.id) {
                list[i] = target
                break
            }
        }

        val tradeLog = TradeLog.builder()
            .wallet(wallet).coin(coin)
            .type(if (transactionType == TransactionType.BUY) TradeType.BUY else TradeType.SELL)
            .quantity(request.quantity)
            .price(txAmount).build()
        tradeLogRepository.save(tradeLog)

        val response = WalletResponse.fromWithValidCoinAmounts(wallet, wallet.coinAmounts.filter(::isValidCoinAmount))
        return ResponseEntity.ok(response)
    }

    fun purchaseItem(userId: Long, request: BuyCoinRequest) =
        processTransaction(userId, request, TransactionType.BUY)

    fun sellItem(userId: Long, request: BuyCoinRequest) =
        processTransaction(userId, request, TransactionType.SELL)

    fun deleteWalletByUserId(userId: Long) {
        walletRepository.findByUserId(userId)?.let { walletRepository.delete(it) }
    }

    fun existsByUserId(userId: Long): Boolean = walletRepository.existsByUserId(userId)
}
