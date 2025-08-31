package com.back.back9.global.redis.initializer;

import com.back.back9.domain.websocket.service.DatabaseCoinListProvider; // 1. 의존성 추가
import com.back.back9.domain.websocket.service.RestService;
import com.back.back9.domain.websocket.vo.CandleInterval;
import com.back.back9.global.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.EnumMap;
import java.util.List; // 🚨 import 추가
import java.util.Map;

import static com.back.back9.domain.websocket.vo.CandleInterval.*;

@Component
@Slf4j
public class RedisInitializer {

    private final RedisService redisService;
    private final RestService RestService;
    private final DatabaseCoinListProvider coinListProvider; // 1. 의존성 추가

    // 🚨 2. 생성자 수정
    public RedisInitializer(RedisService redisService, RestService RestService, DatabaseCoinListProvider coinListProvider) {
        this.redisService = redisService;
        this.RestService = RestService;
        this.coinListProvider = coinListProvider;
    }

    private final Map<CandleInterval, Integer> intervalTargetCount = new EnumMap<>(Map.of(
            SEC, 1000,
            MIN_1, 1000,
            MIN_30, 1000,
            HOUR_1, 1000,
            DAY, 500,
            WEEK, 400,
            MONTH, 200,
            YEAR, 200
    ));

    @EventListener(ApplicationReadyEvent.class)
    public void afterStartup() {
        log.info("✅ Spring Boot 부팅 완료, Redis 데이터 초기화를 시작합니다.");
        log.info("... (WebSocket 연결은 UpbitWebSocketClient가 자동으로 시작합니다)");

        redisService.clearAll();

        // 🚨 3. 핵심 수정 사항: 데이터 초기화 전에 코인 목록 확인
        List<String> marketCodes = coinListProvider.getMarketCodes();
        if (marketCodes.isEmpty()) {
            log.warn("⚠️ 초기화할 코인 목록이 비어있어 Redis 데이터 보충 작업을 건너뜁니다.");
            return; // 데이터 초기화 절차를 시작하지 않고 종료
        }

        // 코인 목록이 있을 때만 백그라운드 스레드에서 데이터 초기화 시작
        new Thread(this::initializeCandles).start();
    }

    private void initializeCandles() {
        log.info("🕒 백그라운드에서 캔들 초기화 시작");

        for (Map.Entry<CandleInterval, Integer> entry : intervalTargetCount.entrySet()) {
            CandleInterval interval = entry.getKey();
            int target = entry.getValue();

            try {
                int inserted = RestService.fetchUntil(interval, target);
                log.info("✅ {} 캔들 {}개 등록 완료 (목표: {})", interval.name(), inserted, target);

            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("⏸️ 429 Too Many Requests 발생: 3분간 전체 수집 중단 후 재시도");
                sleep();
            } catch (Exception e) {
                log.error("❌ {} 캔들 수집 중 심각한 오류 발생: {}", interval.name(), e.getMessage());
            }
        }
        redisService.sortAndRewrite();
        log.info("🎉 전체 캔들 초기화 완료");
    }

    private void sleep() {
        try {
            Thread.sleep(3 * 60 * 1000); // 3분
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}