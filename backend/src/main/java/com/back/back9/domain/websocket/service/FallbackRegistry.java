package com.back.back9.domain.websocket.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FallbackRegistry {
    private final Map<String, Boolean> fallbackMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unused")
    public void enableFallback(String unit) {
        fallbackMap.put(unit, true);
    }

    public boolean isFallback(String unit) {
        return fallbackMap.getOrDefault(unit, false);
    }
}