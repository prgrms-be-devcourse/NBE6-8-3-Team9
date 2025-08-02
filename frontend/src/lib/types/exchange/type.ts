"use client"

import type { MutableRefObject } from "react"

export interface Overlay {
    id: string
    type: "MA" | "EMA" | "BB"
    period: number
    color: string
    visible: boolean
}

export interface CandlestickChartProps {
    data: CandleData[]
    overlays: Overlay[]
    timeframe: string
    onZoomIn: React.MutableRefObject<(() => void) | null>
    onZoomOut: React.MutableRefObject<(() => void) | null>
    onReset: React.MutableRefObject<(() => void) | null>
}

export interface PriceInfo {
    price: number
    time: string
    volume: number
    high: number
    low: number
    open: number
    close: number
    maValues: { [key: string]: number }
}

export interface ChartState {
    visibleCandleCount: number
    maxVisibleCandles: number
    minVisibleCandles: number
    offsetFromEnd: number
    priceRangeMultiplier: number
    isDragging: boolean
    dragStart: { x: number; y: number } | null
    lastMousePos: { x: number; y: number } | null
}

export type ChartArea = "x-axis" | "y-axis" | "chart" | "chart-other" | "outside"

export interface CandleData {
    timestamp: number
    open: number
    high: number
    low: number
    close: number
    volume: number
}

export interface CoinInfo {
    code: string
    name: string
    price: number
    change: number
    changePercent: number
    volume: number
}

export interface ApiCandleData {
    coinCode: string
    candleInfo: {
        timestamp: number
        open: number
        high: number
        low: number
        close: number
        volume: number
    }[]
}

export interface ApiCoinList {
    coins: {
        name: string
        code: string
    }[]
}

export interface WebSocketCandleData {
    timeframe: "1s" | "1m" | "30m" | "1h" | "1d" | "1w" | "1M" | "1y"
    candleInfo: {
        timestamp: number
        open: number
        high: number
        low: number
        close: number
        volume: number
    }
}

export async function fetchHistoricalCandles(coinCode: string, timeframe: string): Promise<ApiCandleData> {
    try {
        const response = await fetch(`/api/candles/${coinCode}?timeframe=${timeframe}`)
        if (!response.ok) {
            throw new Error("Failed to fetch historical candles")
        }
        return await response.json()
    } catch (error) {
        console.error("Error fetching historical candles:", error)
        throw error
    }
}

export async function fetchCoinList(): Promise<ApiCoinList> {
    try {
        const response = await fetch("/api/coins")
        if (!response.ok) {
            throw new Error("Failed to fetch coin list")
        }
        return await response.json()
    } catch (error) {
        console.error("Error fetching coin list:", error)
        throw error
    }
}

export function handleWebSocketMessage(data: WebSocketCandleData, callback: (data: WebSocketCandleData) => void) {
    callback(data)
}
