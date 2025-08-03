package com.back.back9.global.redis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public List<JsonNode> getLatestCandle(String interval, String symbol) {
        String key = symbol + ":" + interval;
        List<String> rawList = redisTemplate.opsForList().range(key, 0, 119);
        if (rawList == null) return List.of();
        return rawList.stream()
                .map(str -> {
                    try {
                        return new ObjectMapper().readTree(str);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("JSON 파싱 실패", e);
                    }
                })
                .collect(Collectors.toList());
    }

    public List<JsonNode> getPreviousCandlesByRange(String interval, String market, int page, String time) {
        String key = market + ":" + interval;
        int PAGE_SIZE = 50;
        int SKIP_SIZE = 120 + page * PAGE_SIZE;

        // 충분히 많은 데이터 가져오기 (최대 1000개까지 저장한다고 가정)
        List<String> rawList = redisTemplate.opsForList().range(key, 0, 999);
        if (rawList == null || rawList.isEmpty()) return List.of();

        ObjectMapper mapper = new ObjectMapper();
        List<JsonNode> parsedList = rawList.stream()
                .map(str -> {
                    try {
                        return mapper.readTree(str);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("JSON 파싱 실패", e);
                    }
                })
                .toList();

        // 기준 시간보다 이전(과거)의 데이터 필터링
        List<JsonNode> filtered = parsedList.stream()
                .filter(node -> {
                    String nodeTime = node.path("timestamp").asText();
                    return nodeTime.compareTo(time) < 0; // 입력시간보다 과거
                })
                .toList();

        // 120개 넘긴 다음 페이지 사이즈만큼 반환
        int fromIndex = Math.min(SKIP_SIZE, filtered.size());
        int toIndex = Math.min(fromIndex + PAGE_SIZE, filtered.size());

        return filtered.subList(fromIndex, toIndex);
    }

    public JsonNode getLatest1sCandle(String coinSymbol) {
        String key = coinSymbol + ":1s";  // 예: "KRW-BTC:1s"
        List<String> list = redisTemplate.opsForList().range(key, 0, 0);
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(list.getFirst());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("실시간 1초 캔들 JSON 파싱 실패", e);
        }
    }

    public void saveCandle(String interval, String market, String candle) {
        String key = market + ":" + interval;
        redisTemplate.opsForList().leftPush(key, candle);
        redisTemplate.opsForList().trim(key, 0, 999); // 최대 1000개
    }

    @SuppressWarnings("unused") // 현재 사용되지 않지만 보존
    public void saveCandle(String msg) {
        try {
            JsonNode node = new ObjectMapper().readTree(msg);
            String market = node.get("code").asText(); // 예: "KRW-BTC"
            String type = node.get("type").asText();   // 예: "candle.1s"
            String interval = type.replace("candle.", ""); // "1s"
            saveCandle(interval, market, msg);
        } catch (Exception e) {
            throw new RuntimeException("RedisService: 실시간 캔들 저장 실패", e);
        }
    }

    public void saveCandleArray(String interval, String market, JsonNode candles) {
        String key = market + ":" + interval;
        List<String> candleList = new ArrayList<>();
        for (JsonNode node : candles) {
            candleList.add(node.toString());
        }
        redisTemplate.opsForList().rightPushAll(key, candleList);
        redisTemplate.opsForList().trim(key, 0, 999);
    }

    public void clearAll() {
        var factory = redisTemplate.getConnectionFactory();
        Optional.ofNullable(factory)
                .map(RedisConnectionFactory::getConnection)
                .ifPresent(connection -> {
                    try (connection) {
                        connection.serverCommands().flushDb();
                    }
                });
    }
}
