package com.back.back9.domain.exchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CoinPriceResponse {
    private String symbol;
    @JsonProperty("trade_price")
    private String price;
    private String time;
}