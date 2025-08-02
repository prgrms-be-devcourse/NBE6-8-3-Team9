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

    public List<JsonNode> getLatestCandle(String key, int count) {
        List<String> rawList = redisTemplate.opsForList().range(key, 0, count - 1);
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

    public List<JsonNode> getPreviousCandles(String interval, String market, int currentSize) {
        String key = market + ":" + interval;
        int endIndex = currentSize + 169;
        List<String> rawList = redisTemplate.opsForList().range(key, currentSize, endIndex);
        if (rawList == null) return List.of();
        return rawList.stream()
                .map(str -> {
                    try {
                        return new ObjectMapper().readTree(str);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("이전 JSON 파싱 실패", e);
                    }
                })
                .collect(Collectors.toList());
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
