package com.back.back9.domain.exchange.service;

import com.back.back9.domain.exchange.dto.CoinPriceResponse;
import com.back.back9.global.redis.service.RedisService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final RedisService redisService;

    public List<JsonNode> getInitialCandles(String interval, String market) {
        String key = market + ":" + interval;
        return redisService.getLatestCandle(key, 170); // 프론트에서는 이 중 120개만 표시
    }

    public List<JsonNode> getPreviousCandles(String interval, String market, int currentSize) {
        return redisService.getPreviousCandles(interval, market, currentSize);
    }

    public CoinPriceResponse getLatestCandleByScan(String coinSymbol) {
        String key = coinSymbol + ":1s";
        List<JsonNode> latestList = redisService.getLatestCandle(key, 1);

        if (latestList.isEmpty()) {
            throw new RuntimeException("최신 캔들 없음: " + coinSymbol);
        }

        JsonNode latest = latestList.getFirst();
        return new CoinPriceResponse(
                latest.path("market").asText(),
                latest.path("candle_date_time_kst").asText(),
                latest.path("trade_price").asText()
        );
    }
}