package com.back.back9.domain.websocket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestCandleScheduler {

    private final FallbackRegistry fallbackRegistry;
    private final UpbitRestCandleFetcher restFetcher;

    @Scheduled(fixedRate = 60_000) // 1분마다
    public void fetchFallbackCandles() {
        if (fallbackRegistry.isFallback("1m")) {
            restFetcher.fetchMinutes(1, 1);
        }
        if (fallbackRegistry.isFallback("30m")) {
            restFetcher.fetchMinutes(30, 1);
        }
        if (fallbackRegistry.isFallback("1h")) {
            restFetcher.fetchMinutes(60, 1);
        }
        restFetcher.fetchInterval("days", 1); // Always REST
        restFetcher.fetchInterval("weeks", 1);
        restFetcher.fetchInterval("months", 1);
        restFetcher.fetchInterval("years", 1);
    }
}