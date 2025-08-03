package com.back.back9.domain.exchange.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ExchangeDTO {
    private final long timestamp;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double volume;

    @JsonCreator
    public ExchangeDTO(
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("opening_price") double open,
            @JsonProperty("high_price") double high,
            @JsonProperty("low_price") double low,
            @JsonProperty("trade_price") double close,
            @JsonProperty("candle_acc_trade_volume") double volume
    ) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }
}
