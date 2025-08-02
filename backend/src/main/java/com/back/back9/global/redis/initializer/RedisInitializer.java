package com.back.back9.global.redis.initializer;

import com.back.back9.domain.websocket.service.UpbitRestCandleFetcher;
import com.back.back9.global.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisInitializer implements ApplicationRunner {

    private final RedisService redisService;
    private final UpbitRestCandleFetcher candleFetcher;

    @Override
    public void run(ApplicationArguments args) {
        redisService.clearAll();
        candleFetcher.fetchInitialOneMinute();    // ✅ 우선 1분 200개 저장
        candleFetcher.fetchAllRemainingInOrder(); // ✅ 이후 나머지 백그라운드로 저장
    }
}
