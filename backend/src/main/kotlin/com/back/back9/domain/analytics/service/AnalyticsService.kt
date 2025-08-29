package com.back.back9.domain.analytics.service

import com.back.back9.domain.analytics.dto.ProfitAnalysisDto
import com.back.back9.domain.analytics.dto.ProfitRateResponse
import com.back.back9.domain.coin.service.CoinService
import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.exchange.service.ExchangeService
import com.back.back9.domain.tradeLog.dto.TradeLogDto
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.tradeLog.service.TradeLogService
import com.back.back9.domain.wallet.service.WalletService
import com.back.back9.global.error.ErrorCode
import com.back.back9.global.error.ErrorException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.stream.Collectors

//package com.back.back9.domain.analytics.service;

@Service
class AnalyticsService(
    private val tradeLogService: TradeLogService,
    private val walletService: WalletService,
    private val exchangeService: ExchangeService,
    private val coinService: CoinService?
) {
    // 실현 손익률 계산
    /*
     * 가상화폐 별 실현 수익률 계산, 실제로 자산을 매도해서 확정된 수익률
     * 1. 지갑 ID에 해당하는 모든 거래 로그 조회 (매수/매도 포함)
     * 2. 거래 로그를 코인 ID별로 그룹화
     * 3. 코인별 수익 분석 결과를 담을 리스트 생성
     * 4. 총 투자금 = walletId에 해당하는 충전 합계 - 출금 합계
     * 5. 코인별로 수익률 계산
     *   5-1. 각 거래 내역을 순회하며 매수/매도별 수량 및 금액 누적
     *   5-2. 평균 매수가 = 총 매수 금액 / 총 매수 수량
     *         // (매수 내역이 없으면 0으로 처리)
     *   5-3. 실현 원가 = 평균 매수가 * 매도 수량
     *   5-4. 실현 수익률 = (총 매도 금액 - 실현 원가) / 총 투자금 * 100
     *   5-5. 분석 결과 객체에 저장
     */
    @Transactional(readOnly = true)
    fun calculateRealizedProfitRates(walletId: Long?): ProfitRateResponse {
        // 지갑에 해당하는 모든 트레이드 로그 조회 (매수, 매도, 충전 포함)
        val tradeLogs: MutableList<TradeLogDto> = tradeLogService.findByWalletId(walletId)
        if (tradeLogs.isEmpty()) {
            log.debug("비어있음" + walletId)
        } else {
            for (tradeLog in tradeLogs) {
                log.info(tradeLog.coinId.toString())
            }
        }


        // 코인별로 트레이드 로그 그룹핑
        val tradeLogsByCoin = tradeLogs.stream()
            .filter { log: TradeLogDto? -> log!!.coinId != null && log.coinId > 0 }
            .collect(Collectors.groupingBy(TradeLogDto::coinId))


        val coinAnalytics: MutableList<ProfitAnalysisDto?> = ArrayList<ProfitAnalysisDto?>()

        // 충전(CHARGE) 로그만 추출하여 총 투자금 계산 (단순 가격 합산)
        val walletLogsTypeCharge: MutableList<TradeLogDto?> = tradeLogService.findByWalletIdAndTypeCharge(walletId)

        val baseInvestment = Money.of(500000000L) // 초기 투자금 (예: 5억 원)
        val walletLogChargeSum = walletLogsTypeCharge.stream()
            .map<Money> { log: TradeLogDto? -> Money.of(log!!.price) }  // 각 로그 금액을 Money로 변환
            .reduce(Money.zero()) { obj: Money?, other: Money? -> obj!!.add(other) }
        val totalInvested = baseInvestment.add(walletLogChargeSum)

        var totalSellAmountSum = Money.zero() // 전체 매도금액 누적
        var totalRealizedCostSum = Money.zero() // 전체 실현 원가 누적

        for (entry in tradeLogsByCoin.entries) {
            val coinId = entry.key
            val coinName: String? = coinId.toString()
            val logs: MutableList<TradeLogDto> = entry.value

            var totalBuyQuantity = BigDecimal.ZERO // 매수 수량 (코인 개수)
            var totalBuyAmount = Money.zero() // 매수 총 금액
            var totalSellQuantity = BigDecimal.ZERO // 매도 수량
            var totalSellAmount = Money.zero() // 매도 총 금액

            // 매수/매도 금액 및 수량 계산
            for (log in logs) {
                // 거래 금액 = 가격 × 수량
                val tradeAmount = Money.of(log.price).multiply(log.quantity)

                if (log.tradeType == TradeType.BUY) {
                    totalBuyQuantity = totalBuyQuantity.add(log.quantity)
                    totalBuyAmount = totalBuyAmount.add(tradeAmount)
                } else if (log.tradeType == TradeType.SELL) {
                    totalSellQuantity = totalSellQuantity.add(log.quantity)
                    totalSellAmount = totalSellAmount.add(tradeAmount)
                }
            }

            // 평균 매수가 = 총 매수 금액 ÷ 총 매수 수량
            val averageBuyPrice = if (totalBuyQuantity.compareTo(BigDecimal.ZERO) > 0)
                totalBuyAmount.divide(totalBuyQuantity)
            else
                Money.zero()

            // 실현 원가 = 평균 매수가 × 매도 수량
            val realizedCost = averageBuyPrice.multiply(totalSellQuantity)

            // 실현 수익 = 매도 총 금액 - 실현 원가
            val realizedProfit = totalSellAmount.subtract(realizedCost)

            // 실현 수익률 = (실현 수익 ÷ 실현 원가) × 100
            val realizedProfitRate = if (realizedCost.isGreaterThanZero())
                realizedProfit.toBigDecimal()
                    .divide(realizedCost.toBigDecimal(), 8, RoundingMode.DOWN)
                    .multiply(BigDecimal.valueOf(100))
            else
                BigDecimal.ZERO

            // 전체 수익 계산을 위한 누적
            totalSellAmountSum = totalSellAmountSum.add(totalSellAmount)
            totalRealizedCostSum = totalRealizedCostSum.add(realizedCost)

            // 현재 보유 수량 = 총 매수 수량 - 총 매도 수량
            val currentHolding = totalBuyQuantity.subtract(totalSellQuantity)

            // DTO에 값 추가 (averageBuyPrice와 profitRate는 그대로 사용)
            coinAnalytics.add(
                ProfitAnalysisDto(
                    coinName,
                    currentHolding,
                    averageBuyPrice.toBigDecimal(),  // DTO가 BigDecimal 받는 경우
                    realizedProfitRate
                )
            )
        }


        // 전체 실현 수익률 = (전체 매도금액 - 전체 실현 원가) / 총 투자금 × 100
        val realizedProfitRateTotal = if (totalInvested.isGreaterThanZero())
            totalSellAmountSum.subtract(totalRealizedCostSum)
                .toBigDecimal()
                .divide(totalInvested.toBigDecimal(), 8, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(100))
        else
            BigDecimal.ZERO

        return ProfitRateResponse(
            walletId,
            coinAnalytics,
            realizedProfitRateTotal,  // 전체 실현 수익률
            realizedProfitRateTotal // 평가 수익률은 이 메서드에서는 계산하지 않음
        )
    }

    // 미실현 손익률 계산
    /*
     * 지갑 내 가상화폐별 미실현 평가 수익률 계산 (실시간 시세 기반), 투자 금액 대비 수익률
     *
     * 1. 현재 wallet에 보유 중인 코인 목록 및 평균 매입 단가, 보유 수량 조회
     * 2. 각 코인에 대해 실시간 현재가 조회
     * 3. 수익률 계산
     *   3-1. 평가금액 = 현재가 * 보유 수량
     *   3-2. 투자원금 = 평균 매입가 * 보유 수량
     *   3-3. 수익률 = (평가금액 - 투자원금) / 투자원금 * 100
     * 4. 코인 ID, 보유 수량, 평균 매입가, 수익률을 분석 결과 객체에 저장
     * 5. 전체 자산 기준 수익률 계산을 위해 총 투자금액, 총 평가금액 누적
     */
    @Transactional(readOnly = true)
    fun calculateUnRealizedProfitRates(walletId: Long?): ProfitRateResponse {
        // 사용자 지갑 내 보유 코인 정보 조회 (코인 ID, 수량, 평균 매수가 등 포함)
        val coinHoldingInfos = walletService.getCoinHoldingsByUserId(walletId)

        val coinAnalytics: MutableList<ProfitAnalysisDto?> = ArrayList<ProfitAnalysisDto?>()

        var totalInvestedAmount = Money.zero() // 총 투자 원금 (코인별 매수가 * 수량)
        var totalEvaluationAmount = Money.zero() // 총 평가 금액 (현재가 *

        for (info in coinHoldingInfos) {
            // 최신 시세 정보 조회
            val coinPriceResponse = exchangeService.getLatestCandleByScan(info.coinName)

            val quantity = info.quantity // 현재 보유 수량
            val avgBuyPrice = Money.of(info.averageBuyPrice) // 평균 매수가
            val currentPrice = Money.of(coinPriceResponse.getPrice()) // 현재가

            log.info(
                "코인: {}, 현재가: {}, 수량: {}, 평균단가: {}",
                info.coinName, currentPrice, quantity, avgBuyPrice
            )

            // 수익률 = (현재가 - 평균단가) / 평균단가 * 100
            val profitRate = if (avgBuyPrice.isGreaterThanZero())
                currentPrice.subtract(avgBuyPrice)
                    .toBigDecimal()
                    .divide(avgBuyPrice.toBigDecimal(), 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
            else
                BigDecimal.ZERO

            // 개별 코인 수익률 분석 정보 추가
            coinAnalytics.add(
                ProfitAnalysisDto(
                    info.coinId.toString(),
                    quantity,
                    avgBuyPrice.toBigDecimal(),  // DTO는 BigDecimal 유지
                    profitRate
                )
            )

            // 총 매수금액 += 평균단가 * 수량
            totalInvestedAmount = totalInvestedAmount.add(avgBuyPrice.multiply(quantity))

            // 총 평가금액 += 현재가 * 수량
            totalEvaluationAmount = totalEvaluationAmount.add(currentPrice.multiply(quantity))
        }

        // 코인 투자 수익률 = (총 평가 - 총 매수) / 총 매수 * 100
        val investmentProfitRate = if (totalInvestedAmount.isGreaterThanZero())
            totalEvaluationAmount.subtract(totalInvestedAmount)
                .toBigDecimal()
                .divide(totalInvestedAmount.toBigDecimal(), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
        else
            BigDecimal.ZERO

        // 현금 포함 자산 정보 조회
        val walletResponse = walletService.getUserWallet(walletId as Long).getBody()
        if (walletResponse == null) {
            throw ErrorException(ErrorCode.WALLET_NOT_FOUND, "null")
        }

        val walletBalance = Money.of(walletResponse.balance) // 지갑 내 현금 잔액

        // (현금 + 매수금액) 대비 평가 수익률 계산
        val totalInvestedWithCash = walletBalance.add(totalInvestedAmount) // 현금 + 총 투자금
        val totalEvaluationWithCash = walletBalance.add(totalEvaluationAmount) // 현금 + 총 평가금

        val totalAssetProfitRate = if (totalInvestedWithCash.isGreaterThanZero())
            totalEvaluationWithCash.subtract(totalInvestedWithCash)
                .toBigDecimal()
                .divide(totalInvestedWithCash.toBigDecimal(), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
        else
            BigDecimal.ZERO

        return ProfitRateResponse(
            walletId,
            coinAnalytics,
            investmentProfitRate,  // 코인 평가 수익률
            totalAssetProfitRate // 현금 포함 총 자산 기준 수익률
        )
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AnalyticsService::class.java)
    }
}
