package com.back.back9.domain.websocket.service;

import com.back.back9.domain.websocket.vo.CandleInterval;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RestCandleScheduler {

    private final FallbackRegistry fallbackRegistry;
    private final UpbitRestCandleFetcher restFetcher;

    public RestCandleScheduler(FallbackRegistry fallbackRegistry, UpbitRestCandleFetcher restFetcher) {
        this.fallbackRegistry = fallbackRegistry;
        this.restFetcher = restFetcher;
    }

    @Scheduled(fixedRate = 60_000)
    public void fetchFallbackCandles() {
        if (fallbackRegistry.isFallback(CandleInterval.MIN_1)) {
            restFetcher.fetchInterval(CandleInterval.MIN_1, 1);
        }
        if (fallbackRegistry.isFallback(CandleInterval.MIN_30)) {
            restFetcher.fetchInterval(CandleInterval.MIN_30, 1);
        }
        if (fallbackRegistry.isFallback(CandleInterval.HOUR_1)) {
            restFetcher.fetchInterval(CandleInterval.HOUR_1, 1);
        }
    }
}