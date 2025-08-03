package com.back.back9.domain.exchange.service;

import com.back.back9.domain.exchange.dto.CoinPriceResponse;
import com.back.back9.global.error.ErrorCode;
import com.back.back9.global.error.ErrorException;
import com.back.back9.global.redis.service.RedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final RedisService redisService;

    private static final Logger log = LoggerFactory.getLogger(ExchangeService.class);

    public List<JsonNode> getInitialCandles(String interval, String market) {
        String key = market + ":" + interval;
        return redisService.getLatestCandle(key, 170); // 프론트에서는 이 중 120개만 표시
    }

    public List<JsonNode> getPreviousCandles(String interval, String market, int currentSize) {
        return redisService.getPreviousCandles(interval, market, currentSize);
    }

    public CoinPriceResponse getLatestCandleByScan(String symbol) {
        String marketCode = CoinSymbolMapper.toMarketCode(symbol);
        log.info("marketCode: {}", marketCode);

        if (marketCode == null) return null;

        try {
            String key = symbol + ":1s";
            List<JsonNode> latestList = redisService.getLatestCandle(key, 1);

            JsonNode latest;

            if (latestList.isEmpty()) {
                // fallback JSON 문자열 → JsonNode로 파싱
                String fallbackJson = "{\"close\": 230000000}";
                latest = new ObjectMapper().readTree(fallbackJson);
            } else {
                latest = latestList.getFirst();
            }

            String close = latest.path("trade_price").asText();
            String now = LocalDateTime.now().toString();

            return new CoinPriceResponse(marketCode, now, close);
        } catch (Exception e) {
            throw new ErrorException(ErrorCode.INTERNAL_ERROR, "Redis 조회 오류: " + e.getMessage());
        }
    }
}