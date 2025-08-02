package com.back.back9.domain.websocket.service;

import com.back.back9.domain.websocket.mock.MockCoinListProvider;
import com.back.back9.global.redis.service.RedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class UpbitRestCandleFetcher {

    private final RedisService redisService;
    private final MockCoinListProvider coinListProvider;
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final int MAX_PER_REQUEST = 200;

    public void fetchInitialOneMinute() {
        fetchMinutes(1, 200); // 최초 1분봉 200개
    }

    public void fetchAllRemainingInOrder() {
        new Thread(() -> {
            fetchSeconds(1000);
            fetchMinutes(30, 1000);
            fetchMinutes(60, 1000);
            fetchInterval("days", 500);
            fetchInterval("weeks", 150);
            fetchInterval("months", 50);
            fetchInterval("years", 10);
        }).start();
    }

    public void fetchMinutes(int unit, int totalCount) {
        for (String market : coinListProvider.getMarketCodes()) {
            for (int i = 0; i < totalCount; i += MAX_PER_REQUEST) {
                delaySafely();
                String url = String.format(
                        "https://api.upbit.com/v1/candles/minutes/%d?market=%s&count=%d",
                        unit, market, Math.min(MAX_PER_REQUEST, totalCount - i)
                );
                fetch(url, "minutes/" + unit, market);
            }
        }
    }

    public void fetchSeconds(int totalCount) {
        for (String market : coinListProvider.getMarketCodes()) {
            for (int i = 0; i < totalCount; i += MAX_PER_REQUEST) {
                delaySafely();
                String url = String.format(
                        "https://api.upbit.com/v1/candles/seconds?market=%s&count=%d",
                        market, Math.min(MAX_PER_REQUEST, totalCount - i)
                );
                fetch(url, "seconds", market);
            }
        }
    }

    public void fetchInterval(String interval, int totalCount) {
        for (String market : coinListProvider.getMarketCodes()) {
            for (int i = 0; i < totalCount; i += MAX_PER_REQUEST) {
                delaySafely();
                String url = String.format(
                        "https://api.upbit.com/v1/candles/%s?market=%s&count=%d",
                        interval, market, Math.min(MAX_PER_REQUEST, totalCount - i)
                );
                fetch(url, interval, market);
            }
        }
    }

    private void fetch(String url, String intervalKey, String market) {
        try {
            String res = rest.getForObject(url, String.class);
            JsonNode arr = mapper.readTree(res);
            redisService.saveCandleArray(intervalKey, market, arr);
        } catch (Exception e) {
            System.err.printf("REST fetch 실패 [%s-%s]: %s%n", intervalKey, market, e.getMessage());
        }
    }

    private void delaySafely() {
        try {
            Thread.sleep(150); // 초당 6~7회로 제한 (안전하게)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
