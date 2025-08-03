"use client"

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