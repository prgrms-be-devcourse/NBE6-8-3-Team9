package com.back.back9.domain.exchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class CoinPriceResponse {
    private String symbol;
    @JsonProperty("trade_price")
    private BigDecimal price;
    private LocalDateTime time;

    public CoinPriceResponse() {
    }

    public CoinPriceResponse(String symbol, BigDecimal price, LocalDateTime time) {
        this.symbol = symbol;
        this.price = price;
        this.time = time;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoinPriceResponse that = (CoinPriceResponse) o;
        return Objects.equals(symbol, that.symbol) && Objects.equals(price, that.price) && Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, price, time);
    }

    @Override
    public String toString() {
        return "CoinPriceResponse{"
                + "symbol='" + symbol + "'"
                + ", price=" + price
                + ", time=" + time
                + '}';
    }
}
