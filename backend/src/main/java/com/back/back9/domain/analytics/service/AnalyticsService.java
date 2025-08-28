package com.back.back9.domain.analytics.service;

import com.back.back9.domain.analytics.dto.ProfitAnalysisDto;
import com.back.back9.domain.analytics.dto.ProfitRateResponse;
import com.back.back9.domain.common.vo.money.Money;
import com.back.back9.domain.exchange.dto.CoinPriceResponse;
import com.back.back9.domain.exchange.service.ExchangeService;
import com.back.back9.domain.tradeLog.entity.TradeLog;
import com.back.back9.domain.tradeLog.entity.TradeType;
import com.back.back9.domain.tradeLog.repository.TradeLogRepository;
import com.back.back9.domain.wallet.dto.CoinHoldingInfo;
import com.back.back9.domain.wallet.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final TradeLogRepository tradeLogRepository;
    private final WalletService walletService;
    private final ExchangeService exchangeService;

    public AnalyticsService(TradeLogRepository tradeLogRepository, WalletService walletService, ExchangeService exchangeService) {
        this.tradeLogRepository = tradeLogRepository;
        this.walletService = walletService;
        this.exchangeService = exchangeService;
    }

    // 실현 손익률 계산
    @Transactional(readOnly = true)
    public ProfitRateResponse calculateRealizedProfitRates(Long walletId) {
        List<TradeLog> tradeLogs = tradeLogRepository.findByWalletId(walletId);

        List<ProfitAnalysisDto> profitAnalyses = tradeLogs.stream()
                .filter(log -> log.getType() == TradeType.SELL)
                .map(sellLog -> {
                    List<TradeLog> buyLogs = tradeLogs.stream()
                            .filter(buyLog -> buyLog.getType() == TradeType.BUY && buyLog.getCoin().equals(sellLog.getCoin()) && !buyLog.getCreatedAt().isAfter(sellLog.getCreatedAt()))
                            .collect(Collectors.toList());

                    Money totalBuyAmount = buyLogs.stream().map(TradeLog::getPrice).reduce(Money.zero(), Money::add);
                    BigDecimal totalBuyQuantity = buyLogs.stream().map(TradeLog::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
                    Money averageBuyPrice = totalBuyAmount.divide(totalBuyQuantity);

                    Money sellAmount = sellLog.getPrice();
                    Money profit = sellAmount.subtract(totalBuyAmount);
                    BigDecimal profitRate = calculateProfitRate(totalBuyAmount, profit);

                    return new ProfitAnalysisDto(
                            sellLog.getCoin().getKoreanName(),
                            sellLog.getQuantity(),
                            averageBuyPrice.toBigDecimal(),
                            profitRate
                    );
                })
                .collect(Collectors.toList());

        BigDecimal averageProfitRate = profitAnalyses.stream()
                .map(ProfitAnalysisDto::realizedProfitRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(profitAnalyses.size()), 4, RoundingMode.HALF_UP);

        return new ProfitRateResponse(walletId, profitAnalyses, averageProfitRate, averageProfitRate);
    }

    // 미실현 손익률 계산
    @Transactional(readOnly = true)
    public ProfitRateResponse calculateUnRealizedProfitRates(Long walletId) {
        List<CoinHoldingInfo> holdings = walletService.getCoinHoldingsForProfitCalculation(walletId);

        List<ProfitAnalysisDto> profitAnalyses = holdings.stream()
                .map(holding -> {
                    CoinPriceResponse latestPriceInfo = exchangeService.getLatestCandleByScan(holding.coinName());
                    Money currentPrice = Money.of(latestPriceInfo.getPrice());
                    Money unrealizedProfit = calculateUnrealizedProfit(holding, currentPrice);
                    BigDecimal unrealizedProfitRate = calculateProfitRate(Money.of(holding.averageBuyPrice()).multiply(holding.quantity()), unrealizedProfit);

                    return new ProfitAnalysisDto(
                            holding.coinName(),
                            holding.quantity(),
                            holding.averageBuyPrice(),
                            unrealizedProfitRate
                    );
                })
                .collect(Collectors.toList());

        BigDecimal averageProfitRate = profitAnalyses.stream()
                .map(ProfitAnalysisDto::realizedProfitRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(profitAnalyses.size()), 4, RoundingMode.HALF_UP);

        return new ProfitRateResponse(walletId, profitAnalyses, averageProfitRate, averageProfitRate);
    }

    private Money getTotalBuyAmountForSell(List<TradeLog> allLogs, TradeLog sellLog) {
        return allLogs.stream()
                .filter(log -> log.getType() == TradeType.BUY && log.getCoin().equals(sellLog.getCoin()) && !log.getCreatedAt().isAfter(sellLog.getCreatedAt()))
                .map(TradeLog::getPrice)
                .reduce(Money.zero(), Money::add);
    }

    private Money calculateUnrealizedProfit(CoinHoldingInfo holding, Money currentPrice) {
        Money totalPurchaseAmount = Money.of(holding.averageBuyPrice()).multiply(holding.quantity());
        Money currentTotalValue = currentPrice.multiply(holding.quantity());
        return currentTotalValue.subtract(totalPurchaseAmount);
    }

    private BigDecimal calculateProfitRate(Money totalBuyAmount, Money profit) {
        if (totalBuyAmount.isZero()) {
            return BigDecimal.ZERO;
        }
        return profit.toBigDecimal().divide(totalBuyAmount.toBigDecimal(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }
}
