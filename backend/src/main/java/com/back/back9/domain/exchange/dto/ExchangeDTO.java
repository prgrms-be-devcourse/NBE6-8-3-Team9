package com.back.back9.domain.exchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@AllArgsConstructor
public class ExchangeDTO {
    @JsonProperty("market")
    private String symbol;
    @JsonProperty("opening_price")
    private String open;
    @JsonProperty("high_price")
    private String high;
    @JsonProperty("low_price")
    private String low;
    @JsonProperty("trade_price")
    private String close;
    @JsonProperty("candle_acc_trade_volume")
    private String volume;

    private String timestamp;
}