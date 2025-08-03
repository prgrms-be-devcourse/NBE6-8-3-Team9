package com.back.back9.domain.exchange.service;

import com.back.back9.domain.exchange.dto.CoinPriceResponse;
import com.back.back9.domain.exchange.dto.ExchangeDTO;
import com.back.back9.global.redis.service.RedisService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.back.back9.standard.util.Ut.json.objectMapper;

@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final RedisService redisService;

    public List<ExchangeDTO> getInitialCandles(String interval, String symbol) {
        List<JsonNode> jsonList = redisService.getLatestCandle(interval, symbol);
        return jsonList.stream()
                .map(json -> objectMapper.convertValue(json, ExchangeDTO.class))
                .collect(Collectors.toList());
    }

    public List<ExchangeDTO> getPreviousCandles(String interval, String market, int page, String time) {
        List<JsonNode> jsonList = redisService.getPreviousCandlesByRange(interval, market, page, time);
        return jsonList.stream()
                .map(json -> objectMapper.convertValue(json, ExchangeDTO.class))
                .collect(Collectors.toList());
    }

    public CoinPriceResponse getLatestCandleByScan(String coinName) {
        try {
            String coinSymbol = CoinSymbolMapper.toMarketCode(coinName);

            JsonNode json = redisService.getLatest1sCandle(coinSymbol);

            if (json == null) {
                // fallback or 기본값 반환
                return new CoinPriceResponse(coinSymbol, LocalDateTime.now().toString(), "0");
            }

            String close = String.valueOf(json.get("trade_price").asText());
            String now = LocalDateTime.now().toString();

            return new CoinPriceResponse(coinSymbol, now, close);
        } catch (Exception e) {
            throw new RuntimeException("getLatestCandleByScan 오류: " + e.getMessage(), e);
        }
    }
}