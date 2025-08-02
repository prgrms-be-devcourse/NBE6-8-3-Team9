package com.back.back9.domain.exchange.controller;

import com.back.back9.domain.exchange.dto.CoinPriceResponse;
import com.back.back9.domain.exchange.service.ExchangeService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exchange")
public class ExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping("/initial")
    public List<JsonNode> getInitialCandles(
            @RequestParam String interval,
            @RequestParam String market
    ) {
        return exchangeService.getInitialCandles(interval, market);
    }

    @GetMapping("/previous")
    public List<JsonNode> getPreviousCandles(
            @RequestParam String interval,
            @RequestParam String market,
            @RequestParam int currentSize
    ) {
        return exchangeService.getPreviousCandles(interval, market, currentSize);
    }

    @GetMapping("/latest-price")
    public CoinPriceResponse getLatestPrice(@RequestParam String market) {
        return exchangeService.getLatestCandleByScan(market);
    }
}