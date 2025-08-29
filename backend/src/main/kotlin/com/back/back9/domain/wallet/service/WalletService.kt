package com.back.back9.domain.wallet.service

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.tradeLog.entity.TradeLog
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.tradeLog.repository.TradeLogRepository
import com.back.back9.domain.user.entity.User
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
        if (walletRepository.findByUserId(userId).isPresent) {
            throw ErrorException(ErrorCode.WALLET_ALREADY_EXISTS, userId)
        }

        val user: User = userRepository.findById(userId)
            .orElseThrow { ErrorException(ErrorCode.USER_NOT_FOUND, userId) }

        val wallet: Wallet = Wallet.builder()
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
        val wallet: Wallet = walletRepository.findByUserId(userId)
            .orElseThrow { ErrorException(ErrorCode.WALLET_NOT_FOUND, userId) }

        val validCoinAmounts = wallet.coinAmounts.filter(::isValidCoinAmount)

        log.info(
            "사용자 지갑 조회 완료 - 사용자 ID: {}, 전체 코인: {}개, 유효한 코인: {}개",
            userId, wallet.coinAmounts.size, validCoinAmounts.size
        )

        val response = WalletResponse.fromWithValidCoinAmounts(wallet, validCoinAmounts)
        return ResponseEntity.ok(response)
    }

    @Transactional
    fun chargeWallet(userId: Long, request: ChargePointsRequest): ResponseEntity<WalletResponse> {
        val wallet: Wallet = walletRepository.findByUserId(userId)
            .orElseThrow { ErrorException(ErrorCode.WALLET_NOT_FOUND, userId) }

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
            .price(chargeAmount)
            .build()
        tradeLogRepository.save(chargeLog)

        log.info(
            "지갑 잔액 충전 완료 - 사용자 ID: {}, 충전 금액: {}, 현재 잔액: {}",
            userId, request.amount, wallet.balance
        )

        val validCoinAmounts = wallet.coinAmounts.filter(::isValidCoinAmount)
        val response = WalletResponse.fromWithValidCoinAmounts(wallet, validCoinAmounts)
        return ResponseEntity.ok(response)
    }

    fun getCoinHoldingsForProfitCalculation(walletId: Long): List<CoinHoldingInfo> {
        val wallet: Wallet = walletRepository.findById(walletId)
            .orElseThrow { ErrorException(ErrorCode.WALLET_NOT_FOUND, walletId) }

        val validCoinAmounts = wallet.coinAmounts.filter(::isValidCoinAmount)
        val coinHoldings = validCoinAmounts.map(CoinHoldingInfo::from)

        log.info("지갑 ID {}의 코인 보유 정보 조회 완료 - 보유 코인 종류: {}개", walletId, coinHoldings.size)
        return coinHoldings
    }

    fun getCoinHoldingsByUserId(userId: Long): List<CoinHoldingInfo> {
        val wallet: Wallet = walletRepository.findByUserId(userId)
            .orElseThrow { ErrorException(ErrorCode.WALLET_NOT_FOUND, userId) }

        return getCoinHoldingsForProfitCalculation(wallet.id)
    }

    private fun isValidCoinAmount(coinAmount: CoinAmount?): Boolean {
        if (coinAmount == null) {
            log.warn("CoinAmount가 null입니다.")
            throw ErrorException(ErrorCode.INVALID_COIN_DATA, "null")
        }
        if (coinAmount.coin == null) {
            log.warn("CoinAmount ID {}의 Coin 정보가 null입니다.", coinAmount.id)
            throw ErrorException(ErrorCode.INVALID_COIN_DATA, coinAmount.id)
        }
        if (coinAmount.coin.id <= 0) {
            log.warn("유효하지 않은 코인 ID: {}", coinAmount.coin.id)
            throw ErrorException(ErrorCode.INVALID_COIN_DATA, coinAmount.id)
        }
        if (coinAmount.coin.symbol == null || coinAmount.coin.symbol.trim().isEmpty()) {
            log.warn("코인 ID {}의 심볼이 비어있습니다.", coinAmount.coin.id)
            throw ErrorException(ErrorCode.INVALID_COIN_DATA, coinAmount.id)
        }
        if (coinAmount.totalAmount == null) {
            log.warn("CoinAmount ID {}의 수량 정보가 null입니다.", coinAmount.id)
            throw ErrorException(ErrorCode.INVALID_COIN_DATA, coinAmount.id)
        }
        if (coinAmount.quantity == null) {
            log.warn("CoinAmount ID {}의 코인 개수 정보가 null입니다.", coinAmount.id)
            throw ErrorException(ErrorCode.INVALID_COIN_DATA, coinAmount.id)
        }
        if (!coinAmount.totalAmount.isGreaterThanZero() && coinAmount.totalAmount != Money.zero()) {
            log.warn("CoinAmount ID {}의 총 금액이 음수입니다: {}", coinAmount.id, coinAmount.totalAmount)
            return false
        }
        if (coinAmount.quantity < BigDecimal.ZERO) {
            log.warn("CoinAmount ID {}의 코인 개수가 음수입니다: {}", coinAmount.id, coinAmount.quantity)
            throw ErrorException(ErrorCode.INVALID_COIN_DATA, coinAmount.id)
        }
        return true
    }

    @Transactional
    fun processTransaction(userId: Long, request: BuyCoinRequest, transactionType: TransactionType): ResponseEntity<WalletResponse> {
        log.info(request.coinId.toString())

        val coin: Coin = coinRepository.findById(request.coinId)
            .orElseThrow { ErrorException(ErrorCode.COIN_NOT_FOUND, request.coinId) }

        val wallet: Wallet = walletRepository.findById(request.walletId)
            .orElseThrow { ErrorException(ErrorCode.WALLET_NOT_FOUND, request.walletId) }

        userRepository.findById(userId)
            .orElseThrow { ErrorException(ErrorCode.USER_NOT_FOUND, userId) }

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
        } else if (transactionType == TransactionType.SELL) {
            wallet.charge(txAmount)
        }

        walletRepository.save(wallet)

        log.info(
            "{} 완료 - 사용자 ID: {}, 코인 ID: {}, 거래 금액: {}, 현재 잔액: {}",
            if (transactionType == TransactionType.BUY) "구매" else "판매",
            userId, coin.id, request.amount, wallet.balance
        )

        var validCoinAmounts = wallet.coinAmounts
            .filter(::isValidCoinAmount)
            .filter { it.coin.id == coin.id }

        if (validCoinAmounts.isEmpty() && transactionType == TransactionType.BUY) {
            log.info("사용자 ID {}의 지갑에 코인 ID {}가 없습니다. 새로운 CoinAmount 생성", userId, coin.id)
            val newCoinAmount: CoinAmount = CoinAmount.builder()
                .coin(coin)
                .wallet(wallet)
                .quantity(BigDecimal.ZERO)
                .totalAmount(Money.of(BigDecimal.ZERO))
                .build()
            coinAmountRepository.save(newCoinAmount)

            wallet.coinAmounts.add(newCoinAmount)
            validCoinAmounts = listOf(newCoinAmount)
        } else if (validCoinAmounts.isEmpty() && transactionType == TransactionType.SELL) {
            throw ErrorException(ErrorCode.INSUFFICIENT_BALANCE, "판매할 코인이 없습니다.")
        }

        val targetCoinAmount = validCoinAmounts[0]
        if (transactionType == TransactionType.BUY) {
            targetCoinAmount.addQuantityAndAmount(request.quantity, txAmount)
        } else if (transactionType == TransactionType.SELL) {
            if (targetCoinAmount.quantity < request.quantity) {
                throw ErrorException(ErrorCode.INSUFFICIENT_BALANCE, "보유 수량이 부족합니다.")
            }
            targetCoinAmount.subtractQuantityAndAmount(request.quantity, txAmount)
        }

        coinAmountRepository.save(targetCoinAmount)

        val walletCoinAmounts = wallet.coinAmounts
        for (i in 0 until walletCoinAmounts.size) {
            if (walletCoinAmounts[i].id == targetCoinAmount.id) {
                walletCoinAmounts[i] = targetCoinAmount
                break
            }
        }

        val tradeLog = TradeLog.builder()
            .wallet(wallet)
            .coin(coin)
            .type(if (transactionType == TransactionType.BUY) TradeType.BUY else TradeType.SELL)
            .quantity(request.quantity)
            .price(txAmount)
            .build()
        tradeLogRepository.save(tradeLog)

        log.info(
            "거래 로그 저장 완료 - 타입: {}, 코인: {}, 수량: {}, 금액: {}",
            if (transactionType == TransactionType.BUY) "구매" else "판매",
            coin.symbol, request.quantity, request.amount
        )

        val response = WalletResponse.fromWithValidCoinAmounts(
            wallet,
            wallet.coinAmounts.filter(::isValidCoinAmount)
        )
        return ResponseEntity.ok(response)
    }

    fun purchaseItem(userId: Long, request: BuyCoinRequest): ResponseEntity<WalletResponse> =
        processTransaction(userId, request, TransactionType.BUY)

    fun sellItem(userId: Long, request: BuyCoinRequest): ResponseEntity<WalletResponse> =
        processTransaction(userId, request, TransactionType.SELL)

    fun deleteWalletByUserId(userId: Long) {
        walletRepository.findByUserId(userId).ifPresent { walletRepository.delete(it) }
    }

    fun existsByUserId(userId: Long): Boolean = walletRepository.existsByUserId(userId)
}
