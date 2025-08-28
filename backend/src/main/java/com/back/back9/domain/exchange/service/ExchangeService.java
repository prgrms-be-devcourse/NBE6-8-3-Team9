package com.back.back9.domain.exchange.service;

import com.back.back9.domain.exchange.dto.CoinPriceResponse;
import com.back.back9.domain.exchange.dto.ExchangeDTO;
import com.back.back9.websocket.service.DatabaseCoinListProvider;
import com.back.back9.domain.websocket.vo.CandleInterval;
import com.back.back9.global.redis.service.RedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExchangeService {

    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final DatabaseCoinListProvider coinListProvider;

    public ExchangeService(RedisService redisService, ObjectMapper objectMapper, DatabaseCoinListProvider coinListProvider) {
        this.redisService = redisService;
        this.objectMapper = objectMapper;
        this.coinListProvider = coinListProvider;
    }

    /**
     * Redis에서 초기 120개 캔들 가져오기
     */
    public List<ExchangeDTO> getInitialCandles(CandleInterval interval, String symbol) {
        List<JsonNode> jsonList = redisService.getLatestCandle(interval, symbol);
        return jsonList.stream()
                .map(json -> {
                    ExchangeDTO dto = objectMapper.convertValue(json, ExchangeDTO.class);
                    // 수정됨: Optional 대신 null 체크
                    String coinName = coinListProvider.getNameBySymbol(dto.getSymbol());
                    if (coinName != null) {
                        dto.setName(coinName);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Redis에서 과거 캔들 페이지 단위로 가져오기 (스크롤)
     */
    public List<ExchangeDTO> getPreviousCandles(CandleInterval interval, String market, int page, LocalDateTime time) {
        List<JsonNode> jsonList = redisService.getPreviousCandlesByRange(interval, market, page, time);
        return jsonList.stream()
                .map(json -> {
                    ExchangeDTO dto = objectMapper.convertValue(json, ExchangeDTO.class);
                    // 수정됨: Optional 대신 null 체크
                    String coinName = coinListProvider.getNameBySymbol(dto.getSymbol());
                    if (coinName != null) {
                        dto.setName(coinName);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Redis에서 1초봉 최신 캔들 1개 스캔 후 반환 (단일 심볼)
     */
    public CoinPriceResponse getLatestCandleByScan(String coinName) {
        try {
            String coinSymbol = CoinSymbolMapper.toMarketCode(coinName);
            JsonNode json = redisService.getLatest1sCandle(coinSymbol);

            if (json == null) {
                return new CoinPriceResponse(coinSymbol, BigDecimal.ZERO, LocalDateTime.now());
            }

            BigDecimal close = json.get("trade_price").decimalValue();
            LocalDateTime now = LocalDateTime.now().withNano(0);

            return new CoinPriceResponse(coinSymbol, close, now);
        } catch (Exception e) {
            throw new RuntimeException("getLatestCandleByScan 오류: " + e.getMessage(), e);
        }
    }

    /**
     * Redis에 저장된 최신 1초 캔들을 기반으로 코인 리스트 전체 조회
     * → MockCoinListProvider에서 한글명 매핑 포함
     */
    public List<ExchangeDTO> getCoinsLatest() {
        List<String> coinlist = coinListProvider.getMarketCodes();
        List<ExchangeDTO> coinInfoList = new ArrayList<>();

        for (String coinSymbol : coinlist) {
            JsonNode jsonNode = redisService.getLatest1sCandle(coinSymbol);
            if (jsonNode != null) {
                ExchangeDTO exchangeDTO = objectMapper.convertValue(jsonNode, ExchangeDTO.class);
                // 수정됨: Optional 대신 null 체크
                String coinName = coinListProvider.getNameBySymbol(coinSymbol);
                if (coinName != null) {
                    exchangeDTO.setName(coinName);
                }
                coinInfoList.add(exchangeDTO);
            }
        }

        return coinInfoList;
    }
}