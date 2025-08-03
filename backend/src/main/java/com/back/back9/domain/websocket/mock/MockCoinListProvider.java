package com.back.back9.domain.websocket.mock;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockCoinListProvider {

    public List<String> getMarketCodes() {
        return List.of(
                "KRW-WAXP", "KRW-CARV", "KRW-LSK", "KRW-BORA", "KRW-PUNDIX",
                "KRW-BAT", "KRW-HUNT", "KRW-PENGU", "KRW-FIL", "KRW-BEAM",
                "KRW-WAVES", "KRW-USDC", "KRW-MOVE", "KRW-AERGO", "KRW-USDT",
                "KRW-BOUNTY", "KRW-KAITO", "KRW-LPT", "KRW-BLAST", "KRW-DKA",
                "KRW-ALGO", "KRW-SHIB", "KRW-UNI", "KRW-TOKAMAK", "KRW-DOGE",
                "KRW-PEPE", "KRW-HBAR", "KRW-NEWT", "KRW-SEI", "KRW-BONK"
        );
    }
}
