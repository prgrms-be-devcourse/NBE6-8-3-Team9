package com.back.back9.domain.exchange.dto;

import com.back.back9.domain.websocket.vo.CandleInterval;

public class InitialRequestDTO {
    private CandleInterval interval;
    private String market;

    public CandleInterval getInterval() {
        return interval;
    }

    public void setInterval(CandleInterval interval) {
        this.interval = interval;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }
}
