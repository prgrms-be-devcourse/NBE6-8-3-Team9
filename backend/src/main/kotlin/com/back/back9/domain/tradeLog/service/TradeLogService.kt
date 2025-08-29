package com.back.back9.domain.tradeLog.service

import com.back.back9.domain.wallet.entity.Wallet
//import mu.KotlinLogging
import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.tradeLog.dto.TradeLogDto
import com.back.back9.domain.tradeLog.dto.TradeLogDto.Companion.from
import com.back.back9.domain.tradeLog.dto.TradeLogDto.Companion.toEntity
import com.back.back9.domain.tradeLog.entity.TradeLog
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.tradeLog.repository.TradeLogRepository
import com.back.back9.domain.wallet.repository.WalletRepository
import com.back.back9.global.error.ErrorCode
import com.back.back9.global.error.ErrorException
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

//private val log = KotlinLogging.logger {}

@Service
class TradeLogService(
    private val tradeLogRepository: TradeLogRepository,
    private val walletRepository: WalletRepository,
    private val coinRepository: CoinRepository
) {
    @Transactional(readOnly = true)
    fun findAll(): List<TradeLog?> =
        tradeLogRepository.findAll()

    @Transactional(readOnly = true)
    fun findByWalletId(userId: Long?): List<TradeLogDto> {
        val wallet = walletRepository.findByUserId(userId)
            ?: throw ErrorException(ErrorCode.WALLET_NOT_FOUND, userId)

//        임시
        return tradeLogRepository.findByWalletId(1L)
            .map { from(it) }

//        return tradeLogRepository.findByWalletId(wallet.getId())
//            .map { from(it) }

    }

    @Transactional(readOnly = true)
    fun findByFilter(
        walletId: Long,
        type: TradeType?,
        coinId: Int?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        pageable: Pageable?
    ): Page<TradeLogDto> =
        tradeLogRepository.findByWalletIdFilter(walletId, type, coinId, startDate, endDate, pageable)
            .map { from(it) }

    @Transactional(readOnly = true)
    fun findByUserIdAndFilter(
        userId: Long?,
        type: TradeType?,
        coinId: Int?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        pageable: Pageable?
    ): Page<TradeLogDto?> {
        val wallet = walletRepository.findByUserId(userId)
            ?: throw ErrorException(ErrorCode.WALLET_NOT_FOUND, userId)
//임시
        return tradeLogRepository.findByWalletIdFilter(1L, type, coinId, startDate, endDate, pageable)
            .map { from(it) }
//        return tradeLogRepository.findByWalletIdFilter(wallet.id!!, type, coinId, startDate, endDate, pageable)
//            .map { from(it) }
    }

    @Transactional(readOnly = true)
    fun findByWalletIdAndTypeCharge(walletId: Long?): List<TradeLogDto> =
        findByWalletId(walletId)
            .filter { it.tradeType == TradeType.CHARGE }

    @Transactional(readOnly = true)
    fun count(): Int = tradeLogRepository.count().toInt()

    @Transactional
    fun saveAll(tradeLogs: List<TradeLog>): List<TradeLog> =
        tradeLogRepository.saveAll(tradeLogs)

    @Transactional
    fun save(tradeLogDto: TradeLogDto): TradeLogDto {
        val wallet = walletRepository.findById(tradeLogDto.walletId!!)
            .orElseThrow { EntityNotFoundException("Wallet not found") }

        val coin = coinRepository.findById(tradeLogDto.coinId!!)
            .orElseThrow { EntityNotFoundException("Coin not found") }

        val savedTradeLog = tradeLogRepository.save(toEntity(tradeLogDto, wallet, coin))
        return from(savedTradeLog)
    }

    @Transactional
    fun save(tradeLog: TradeLog): TradeLog =
        tradeLogRepository.save(tradeLog)

    @Transactional
    fun createMockLogs() {
        if (count() > 0) return

        coinRepository.deleteAll()
        val wallet = walletRepository.findById(1L)
            .orElseThrow { RuntimeException("wallet not found") }

        val coin1 = coinRepository.save(Coin("비트코인1", "Bitcoin1", "BTC1"))
        val coin2 = coinRepository.save(Coin("이더리움1", "Ethereum1", "ETH1"))
        val coin3 = coinRepository.save(Coin("리플1", "Ripple1", "XRP1"))

        val baseDate = LocalDateTime.of(2025, 7, 25, 0, 0)

        val logs = (1..15).map { i ->
            val coin = if (i <= 9) coin1 else coin2
            val type = if (i % 3 == 0) TradeType.SELL else TradeType.BUY

            TradeLog(
                wallet = wallet,
                coin = coin,
                type = type,
                quantity = BigDecimal.ONE,
                price = Money.of(100_000_000L + (i * 10_000_000L))
            ).apply {
                createdAt = baseDate.plusDays(((i - 1) * 7).toLong())
            }
        }

        saveAll(logs)
    }
}
