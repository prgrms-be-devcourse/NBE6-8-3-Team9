import { useCallback, useEffect, useRef, useState } from "react";
import type { CandleInterval, ExchangeDTO } from "@/lib/types/exchange/type";
import { exchangeApi } from "@/lib/api/exchange";

export const useCandleData = (market: string, interval: CandleInterval) => {
    const [displayedCandles, setDisplayedCandles] = useState<ExchangeDTO[]>([]);
    const pageRef = useRef(0);
    const initialCandlesRef = useRef<ExchangeDTO[]>([]);
    const unrenderedCandlesRef = useRef<ExchangeDTO[]>([]);

    // ---------------------------
    // interval -> ms 변환
    // ---------------------------
    const intervalToMs = (interval: CandleInterval): number => {
        switch (interval) {
            case "SEC": return 1000;
            case "MIN_1": return 60 * 1000;
            case "MIN_30": return 30 * 60 * 1000;
            case "HOUR_1": return 60 * 60 * 1000;
            case "DAY": return 24 * 60 * 60 * 1000;
            case "WEEK": return 7 * 24 * 60 * 60 * 1000;
            case "MONTH": return 30 * 24 * 60 * 60 * 1000;
            case "YEAR": return 365 * 24 * 60 * 60 * 1000;
            default: return 60 * 1000;
        }
    };

    // ---------------------------
    // 캔들 정규화 함수
    // ---------------------------
    const normalizeCandlesByInterval = (candles: ExchangeDTO[], interval: CandleInterval): ExchangeDTO[] => {
        const ms = intervalToMs(interval);
        const result: ExchangeDTO[] = [];
        let currentCandle: ExchangeDTO | null = null;
        let currentTimestamp: number | null = null;

        candles.forEach(c => {
            const time = new Date(c.candle_date_time_kst).getTime();
            const normalizedTime = Math.floor(time / ms) * ms;

            if (normalizedTime !== currentTimestamp) {
                if (currentCandle) result.push(currentCandle);
                currentCandle = { ...c, candle_date_time_kst: new Date(normalizedTime).toISOString() };
                currentTimestamp = normalizedTime;
            } else if (currentCandle) {
                currentCandle.high_price = Math.max(currentCandle.high_price, c.high_price);
                currentCandle.low_price = Math.min(currentCandle.low_price, c.low_price);
                currentCandle.trade_price = c.trade_price; // 마지막 가격
                currentCandle.candle_acc_trade_volume += c.candle_acc_trade_volume;
            }
        });

        if (currentCandle) result.push(currentCandle);
        return result;
    };

    const loadInitial = useCallback(async () => {
        try {
            const data = await exchangeApi.getInitialCandles({ market, interval });
            const reversed = data.slice().reverse();
            const normalized = normalizeCandlesByInterval(reversed, interval);
            initialCandlesRef.current = normalized;
            pageRef.current = 1;

            const latest = normalized.slice(-50);
            const unrendered = normalized.slice(0, normalized.length - 50);

            setDisplayedCandles(latest);
            unrenderedCandlesRef.current = unrendered;
        } catch (e) {
            console.error("loadInitial failed:", e);
        }
    }, [market, interval]);

    const loadPrevious = useCallback(async () => {
        try {
            const oldest = displayedCandles[0];
            const fetched = await exchangeApi.getPreviousCandles({
                market,
                interval,
                page: pageRef.current,
                time: oldest.candle_date_time_kst,
            });
            if (!fetched.length) return;

            pageRef.current += 1;
            const reversed = fetched.slice().reverse();
            const normalized = normalizeCandlesByInterval(reversed, interval);
            setDisplayedCandles(prev => [...normalized, ...prev]);
        } catch (e) {
            console.error("loadPrevious failed:", e);
        }
    }, [displayedCandles, market, interval]);

    const resetCandles = useCallback(() => {
        setDisplayedCandles([]);
        pageRef.current = 0;
        initialCandlesRef.current = [];
        unrenderedCandlesRef.current = [];
    }, []);

    const changeInterval = useCallback((newInterval: CandleInterval) => {
        resetCandles();
    }, [resetCandles]);

    useEffect(() => {
        loadInitial();
    }, [loadInitial]);

    return {
        candles: displayedCandles,
        loadPrevious,
        resetCandles,
        changeInterval,
    };
};
