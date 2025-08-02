"use client"

import { useState, useEffect, useCallback, useMemo, useRef } from "react"
import { Card, CardContent } from "@/components/ui/card"
import { useUpbitWebSocket } from "@/hooks/use-upbit-websocket"
import type { CandleData, CoinInfo, Overlay } from "@/lib/types/exchange/type"
import ConnectionStatus from "@/components/exchange/connection-status"
import PriceHeader from "@/components/exchange/price-header"
import ChartToolbar from "@/components/exchange/chart-toolbar"
import CandlestickChart from "@/components/exchange/candlestick-chart"
import CoinList from "@/components/exchange/coin-list"

const INTERVAL_MAP: Record<string, number> = {
    "1s": 1000, "1m": 60000, "30m": 1800000, "1h": 3600000,
    "1d": 86400000, "1w": 604800000, "1M": 2592000000, "1y": 31536000000
}

const MOCK_COIN_LIST: CoinInfo[] = [
    { code: "KRW-BTC", name: "비트코인", price: 163415000, change: 297000, changePercent: 0.18, volume: 274.634 },
    { code: "KRW-XRP", name: "엑스알피(리플)", price: 4144, change: -68, changePercent: -1.61, volume: 531.982 },
    { code: "KRW-ETH", name: "이더리움", price: 5092000, change: -61000, changePercent: -1.18, volume: 326.53 },
    { code: "KRW-ADA", name: "에이다", price: 787, change: -8, changePercent: -1.01, volume: 189.87 },
    { code: "KRW-DOGE", name: "도지코인", price: 287, change: -5, changePercent: -1.71, volume: 99.291 }
]

export default function TradingPage() {
    const [selectedCoin, setSelectedCoin] = useState("KRW-BTC")
    const [selectedTimeframe, setSelectedTimeframe] = useState("1m")
    const [candleData, setCandleData] = useState<CandleData[]>([])
    const [coinList] = useState(MOCK_COIN_LIST)
    const [currentPrice, setCurrentPrice] = useState<CoinInfo | null>(null)
    const [userRole] = useState("admin")
    const [overlays, setOverlays] = useState<Overlay[]>([
        { id: "ma50", type: "MA", period: 50, color: "#ef4444", visible: true },
        { id: "ma15", type: "MA", period: 15, color: "#3b82f6", visible: true }
    ])

    const chartZoomInRef = useRef(null as unknown) as React.MutableRefObject<(() => void) | null>
    const chartZoomOutRef = useRef(null as unknown) as React.MutableRefObject<(() => void) | null>
    const chartResetRef = useRef(null as unknown) as React.MutableRefObject<(() => void) | null>

    const currentInterval = useMemo(() => INTERVAL_MAP[selectedTimeframe] || 60000, [selectedTimeframe])

    const generateMockCandleData = useCallback((): CandleData[] => {
        const data: CandleData[] = []
        let basePrice = 163000000
        const now = Date.now()
        for (let i = 999; i >= 0; i--) {
            const timestamp = now - i * currentInterval
            const open = basePrice + (Math.random() - 0.5) * 2000000
            const close = open + (Math.random() - 0.5) * 1000000
            const high = Math.max(open, close) + Math.random() * 500000
            const low = Math.min(open, close) - Math.random() * 500000
            const volume = Math.random() * 10
            data.push({ timestamp, open, high, low, close, volume })
            basePrice = close + (Math.random() - 0.5) * 100000
        }
        return data
    }, [currentInterval])

    const handleCandleUpdate = useCallback((newCandle: CandleData) => {
        setCandleData(prev => {
            const updated = [...prev]
            const last = updated[updated.length - 1]
            const newKey = Math.floor(newCandle.timestamp / currentInterval)
            const lastKey = last ? Math.floor(last.timestamp / currentInterval) : -1
            if (newKey === lastKey) updated[updated.length - 1] = newCandle
            else {
                updated.push(newCandle)
                if (updated.length > 1000) updated.shift()
            }
            return updated
        })

        setCurrentPrice(prev => {
            if (!prev) return null
            const change = newCandle.close - newCandle.open
            return {
                ...prev,
                price: newCandle.close,
                change,
                changePercent: (change / newCandle.open) * 100
            }
        })
    }, [currentInterval])

    const { isConnected, connectionStatus } = useUpbitWebSocket({
        symbols: [selectedCoin],
        timeframe: selectedTimeframe,
        onCandleUpdate: handleCandleUpdate,
        onError: console.error
    })

    useEffect(() => {
        setCandleData(generateMockCandleData())
        setCurrentPrice(MOCK_COIN_LIST[0])
    }, [])

    useEffect(() => {
        setCandleData(generateMockCandleData())
    }, [selectedTimeframe, generateMockCandleData])

    useEffect(() => {
        const interval = setInterval(() => {
            const now = Date.now()
            const last = candleData[candleData.length - 1]
            const open = (last?.close || 163000000) + (Math.random() - 0.5) * 100000
            const close = open + (Math.random() - 0.5) * 500000
            const high = Math.max(open, close) + Math.random() * 200000
            const low = Math.min(open, close) - Math.random() * 200000
            handleCandleUpdate({
                timestamp: now,
                open,
                high,
                low,
                close,
                volume: Math.random() * 10
            })
        }, Math.max(currentInterval, 1000))
        return () => clearInterval(interval)
    }, [currentInterval, handleCandleUpdate])

    const handleCoinSelect = useCallback((coin: CoinInfo) => {
        setSelectedCoin(coin.code)
        setCurrentPrice(coin)
    }, [])

    const handleTimeframeChange = useCallback((timeframe: string) => {
        setSelectedTimeframe(timeframe)
    }, [])

    const handleAddOverlay = useCallback((period: number) => {
        setOverlays(prev => [...prev, {
            id: `ma${period}_${Date.now()}`,
            type: "MA",
            period,
            color: `hsl(${Math.random() * 360}, 70%, 50%)`,
            visible: true
        }])
    }, [])

    const handleRemoveOverlay = useCallback((id: string) => {
        setOverlays(prev => prev.filter(o => o.id !== id))
    }, [])

    const handleToggleOverlay = useCallback((id: string) => {
        setOverlays(prev => prev.map(o => o.id === id ? { ...o, visible: !o.visible } : o))
    }, [])

    const handleReset = useCallback(() => {
        setSelectedTimeframe("1m")
        setOverlays([
            { id: "ma50", type: "MA", period: 50, color: "#ef4444", visible: true },
            { id: "ma15", type: "MA", period: 15, color: "#3b82f6", visible: true }
        ])
        chartResetRef.current?.()
    }, [])

    const handleZoomIn = useCallback(() => chartZoomInRef.current?.(), [])
    const handleZoomOut = useCallback(() => chartZoomOutRef.current?.(), [])

    return (
        <div className="bg-gray-50 min-h-screen">
            <div className="max-w-7xl mx-auto">
                <div className="bg-white border-b px-4 py-4">
                    <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center space-x-4">
                            <h1 className="text-2xl font-bold">
                                비트코인 <span className="text-sm text-gray-500">BTC/KRW</span>
                            </h1>
                            <ConnectionStatus status={connectionStatus} isConnected={isConnected} />
                        </div>
                    </div>
                    {currentPrice && <PriceHeader coinInfo={currentPrice} />}
                </div>

                <div className="flex">
                    <div className="flex-1">
                        <Card className="rounded-none border-0 border-r">
                            <ChartToolbar
                                selectedTimeframe={selectedTimeframe}
                                onTimeframeChange={handleTimeframeChange}
                                onZoomIn={handleZoomIn}
                                onZoomOut={handleZoomOut}
                                onReset={handleReset}
                                overlays={overlays}
                                onAddOverlay={handleAddOverlay}
                                onRemoveOverlay={handleRemoveOverlay}
                                onToggleOverlay={handleToggleOverlay}
                            />
                            <CardContent className="p-0">
                                <CandlestickChart
                                    data={candleData}
                                    overlays={overlays}
                                    timeframe={selectedTimeframe}
                                    onZoomIn={chartZoomInRef}
                                    onZoomOut={chartZoomOutRef}
                                    onReset={chartResetRef}
                                />
                            </CardContent>
                        </Card>
                    </div>

                    <div className="w-80">
                        <CoinList
                            coins={coinList}
                            selectedCoin={selectedCoin}
                            onCoinSelect={handleCoinSelect}
                            userRole={userRole}
                        />
                    </div>
                </div>
            </div>
        </div>
    )
}
