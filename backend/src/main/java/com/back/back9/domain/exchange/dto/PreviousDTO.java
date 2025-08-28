package com.back.back9.domain.exchange.dto;

import com.back.back9.domain.websocket.vo.CandleInterval;

import java.time.LocalDateTime;

public class PreviousDTO {
    private CandleInterval interval;
    private String market;
    private int page;
    private LocalDateTime time;

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

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
