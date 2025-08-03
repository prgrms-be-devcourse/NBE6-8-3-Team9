package com.back.back9.domain.websocket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpbitRestScheduler {

    private final UpbitRestCandleFetcher restFetcher;

    @Scheduled(fixedDelay = 1000 * 60 * 5) // 5분마다 실행
    public void scheduledFetchRest() {
        restFetcher.fetchMinutes(30, 200);
        restFetcher.fetchMinutes(60, 200);
        restFetcher.fetchInterval("days", 200);
        restFetcher.fetchInterval("weeks", 100);
        restFetcher.fetchInterval("months", 50);
        restFetcher.fetchInterval("years", 10);
    }
}
