"use client"

import type React from "react"
import { useEffect, useRef, useState, useCallback } from "react"
import {CandleData, CandlestickChartProps, ChartArea, ChartState, PriceInfo} from "@/lib/types/exchange/type";

export default function CandlestickChart({
                                             data,
                                             overlays,
                                             timeframe,
                                             onZoomIn,
                                             onZoomOut,
                                             onReset,
                                         }: CandlestickChartProps) {
    const canvasRef = useRef<HTMLCanvasElement>(null)
    const containerRef = useRef<HTMLDivElement>(null)
    const [hoverInfo, setHoverInfo] = useState<PriceInfo | null>(null)
    const [mousePosition, setMousePosition] = useState({ x: 0, y: 0 })
    const [currentArea, setCurrentArea] = useState<ChartArea>("outside")
    const [chartState, setChartState] = useState<ChartState>({
        visibleCandleCount: 120,
        maxVisibleCandles: 1000,
        minVisibleCandles: 10,
        offsetFromEnd: 0,
        priceRangeMultiplier: 1.0,
        isDragging: false,
        dragStart: null,
        lastMousePos: null,
    })

    // Calculate moving average
    const calculateMA = useCallback((data: CandleData[], period: number): number[] => {
        const ma: number[] = []
        for (let i = 0; i < data.length; i++) {
            if (i < period - 1) {
                ma.push(Number.NaN)
            } else {
                const sum = data.slice(i - period + 1, i + 1).reduce((acc, candle) => acc + candle.close, 0)
                ma.push(sum / period)
            }
        }
        return ma
    }, [])

    // External control functions
    const zoomIn = useCallback(() => {
        setChartState((prev) => ({
            ...prev,
            visibleCandleCount: Math.max(prev.minVisibleCandles, Math.round(prev.visibleCandleCount * 0.8)),
        }))
    }, [])

    const zoomOut = useCallback(() => {
        setChartState((prev) => ({
            ...prev,
            visibleCandleCount: Math.min(prev.maxVisibleCandles, Math.round(prev.visibleCandleCount * 1.25)),
        }))
    }, [])

    const resetChart = useCallback(() => {
        setChartState({
            visibleCandleCount: 120,
            maxVisibleCandles: 1000,
            minVisibleCandles: 10,
            offsetFromEnd: 0,
            priceRangeMultiplier: 1.0,
            isDragging: false,
            dragStart: null,
            lastMousePos: null,
        })
    }, [])

    // Expose functions to parent
    useEffect(() => {
        if (onZoomIn?.current !== undefined) onZoomIn.current = zoomIn
        if (onZoomOut?.current !== undefined) onZoomOut.current = zoomOut
        if (onReset?.current !== undefined) onReset.current = resetChart
    }, [zoomIn, zoomOut, resetChart, onZoomIn, onZoomOut, onReset])

    // Get visible data based on current chart state
    const getVisibleData = useCallback(() => {
        if (data.length === 0) return []

        const endIndex = data.length - 1 - chartState.offsetFromEnd
        const startIndex = Math.max(0, endIndex - chartState.visibleCandleCount + 1)

        return data.slice(startIndex, endIndex + 1)
    }, [data, chartState.visibleCandleCount, chartState.offsetFromEnd])

    // Determine which area the mouse is in
    const getMouseArea = useCallback((mouseX: number, mouseY: number, rect: DOMRect): ChartArea => {
        const padding = 40
        const isOnXAxis = mouseY > rect.height - padding
        const isOnYAxis = mouseX < padding
        const isInChartArea =
            mouseX >= padding && mouseX <= rect.width - padding && mouseY >= padding && mouseY <= rect.height - padding

        if (isOnXAxis) return "x-axis"
        if (isOnYAxis) return "y-axis"
        if (isInChartArea) return "chart"
        return "chart-other"
    }, [])

    // Handle mouse wheel for different areas
    const handleWheel = useCallback(
        (event: React.WheelEvent<HTMLCanvasElement>) => {
            const canvas = canvasRef.current
            if (!canvas) return

            const rect = canvas.getBoundingClientRect()
            const mouseX = event.clientX - rect.left
            const mouseY = event.clientY - rect.top
            const area = getMouseArea(mouseX, mouseY, rect)

            setCurrentArea(area)

            const delta = event.deltaY < 0 ? 1.1 : 0.9

            switch (area) {
                case "x-axis":
                    // X축 영역: 시간축 줌인/아웃
                    event.preventDefault()
                    event.stopPropagation()
                    setChartState((prev) => ({
                        ...prev,
                        visibleCandleCount: Math.max(
                            prev.minVisibleCandles,
                            Math.min(prev.maxVisibleCandles, Math.round(prev.visibleCandleCount * delta)),
                        ),
                    }))
                    break

                case "y-axis":
                    // Y축 영역: 가격축 줌인/아웃
                    event.preventDefault()
                    event.stopPropagation()
                    setChartState((prev) => ({
                        ...prev,
                        priceRangeMultiplier: Math.max(0.1, Math.min(10, prev.priceRangeMultiplier * delta)),
                    }))
                    break

                case "chart":
                    // 그래프 영역: 시간대 조절 (과거/실시간 이동)
                    event.preventDefault()
                    event.stopPropagation()
                    const timeOffset = event.deltaY > 0 ? 5 : -5
                    setChartState((prev) => ({
                        ...prev,
                        offsetFromEnd: Math.max(0, Math.min(data.length - 1, prev.offsetFromEnd + timeOffset)),
                    }))
                    break

                case "chart-other":
                    // 차트 내부 기타 영역: 페이지 스크롤 차단하지 않음
                    // 이벤트를 그대로 통과시켜 페이지 스크롤 허용
                    break

                default:
                    // 차트 외부: 페이지 스크롤 허용 (이벤트를 그대로 통과)
                    break
            }
        },
        [getMouseArea, data.length],
    )

    // Handle mouse down for dragging (only in chart area)
    const handleMouseDown = useCallback(
        (event: React.MouseEvent<HTMLCanvasElement>) => {
            const canvas = canvasRef.current
            if (!canvas) return

            const rect = canvas.getBoundingClientRect()
            const mouseX = event.clientX - rect.left
            const mouseY = event.clientY - rect.top
            const area = getMouseArea(mouseX, mouseY, rect)

            // 그래프 영역에서만 드래그 허용
            if (area === "chart") {
                setChartState((prev) => ({
                    ...prev,
                    isDragging: true,
                    dragStart: { x: mouseX, y: mouseY },
                    lastMousePos: { x: mouseX, y: mouseY },
                }))
            }
        },
        [getMouseArea],
    )

    // Handle mouse move
    const handleMouseMove = useCallback(
        (event: React.MouseEvent<HTMLCanvasElement>) => {
            const canvas = canvasRef.current
            if (!canvas) return

            const rect = canvas.getBoundingClientRect()
            const mouseX = event.clientX - rect.left
            const mouseY = event.clientY - rect.top
            const area = getMouseArea(mouseX, mouseY, rect)

            setMousePosition({ x: mouseX, y: mouseY })
            setCurrentArea(area)

            if (chartState.isDragging && chartState.lastMousePos && area === "chart") {
                const deltaX = mouseX - chartState.lastMousePos.x
                const padding = 40
                const chartWidth = rect.width - padding * 2
                const candlesPerPixel = chartState.visibleCandleCount / chartWidth
                const candleOffset = Math.round(deltaX * candlesPerPixel)

                setChartState((prev) => ({
                    ...prev,
                    offsetFromEnd: Math.max(0, Math.min(data.length - 1, prev.offsetFromEnd + candleOffset)),
                    lastMousePos: { x: mouseX, y: mouseY },
                }))
            } else if (area === "chart") {
                // 호버 정보 업데이트 (차트 영역에서만)
                const visibleData = getVisibleData()
                if (visibleData.length === 0) return

                const padding = 40
                const chartWidth = rect.width - padding * 2
                const candleSpacing = chartWidth / visibleData.length
                const candleIndex = Math.floor((mouseX - padding) / candleSpacing)

                if (candleIndex >= 0 && candleIndex < visibleData.length) {
                    const candle = visibleData[candleIndex]
                    const maValues: { [key: string]: number } = {}

                    // Calculate MA values at this point
                    overlays.forEach((overlay) => {
                        if (overlay.visible && overlay.type === "MA") {
                            const actualIndex = data.length - chartState.offsetFromEnd - visibleData.length + candleIndex
                            const maData = calculateMA(data, overlay.period)
                            if (actualIndex >= 0 && actualIndex < maData.length && !isNaN(maData[actualIndex])) {
                                maValues[`MA${overlay.period}`] = maData[actualIndex]
                            }
                        }
                    })

                    setHoverInfo({
                        price: candle.close,
                        time: new Date(candle.timestamp).toLocaleString("ko-KR"),
                        volume: candle.volume,
                        high: candle.high,
                        low: candle.low,
                        open: candle.open,
                        close: candle.close,
                        maValues,
                    })
                }
            } else {
                setHoverInfo(null)
            }
        },
        [
            chartState.isDragging,
            chartState.lastMousePos,
            chartState.visibleCandleCount,
            chartState.offsetFromEnd,
            data,
            getVisibleData,
            overlays,
            calculateMA,
            getMouseArea,
        ],
    )

    // Handle mouse up
    const handleMouseUp = useCallback(() => {
        setChartState((prev) => ({
            ...prev,
            isDragging: false,
            dragStart: null,
            lastMousePos: null,
        }))
    }, [])

    const handleMouseLeave = useCallback(() => {
        setCurrentArea("outside")
        setHoverInfo(null)
        setChartState((prev) => ({
            ...prev,
            isDragging: false,
            dragStart: null,
            lastMousePos: null,
        }))
    }, [])

    // 실시간 데이터 업데이트 시 자동으로 최신 데이터로 이동
    useEffect(() => {
        if (chartState.offsetFromEnd === 0) {
            setChartState((prev) => ({ ...prev, offsetFromEnd: 0 }))
        }
    }, [data.length])

    useEffect(() => {
        const canvas = canvasRef.current
        if (!canvas) return

        const handleGlobalMouseUp = () => handleMouseUp()
        const handleGlobalMouseMove = (e: MouseEvent) => {
            if (chartState.isDragging) {
                const rect = canvas.getBoundingClientRect()
                const mouseX = e.clientX - rect.left
                const mouseY = e.clientY - rect.top

                if (chartState.lastMousePos) {
                    const deltaX = mouseX - chartState.lastMousePos.x
                    const padding = 40
                    const chartWidth = rect.width - padding * 2
                    const candlesPerPixel = chartState.visibleCandleCount / chartWidth
                    const candleOffset = Math.round(deltaX * candlesPerPixel)

                    setChartState((prev) => ({
                        ...prev,
                        offsetFromEnd: Math.max(0, Math.min(data.length - 1, prev.offsetFromEnd + candleOffset)),
                        lastMousePos: { x: mouseX, y: mouseY },
                    }))
                }
            }
        }

        document.addEventListener("mouseup", handleGlobalMouseUp)
        document.addEventListener("mousemove", handleGlobalMouseMove)

        return () => {
            document.removeEventListener("mouseup", handleGlobalMouseUp)
            document.removeEventListener("mousemove", handleGlobalMouseMove)
        }
    }, [chartState.isDragging, chartState.lastMousePos, chartState.visibleCandleCount, data.length, handleMouseUp])

    // 네이티브 wheel 이벤트 처리 - 축 영역에서 직접 처리
    useEffect(() => {
        const container = containerRef.current
        if (!container) return

        const handleNativeWheel = (e: WheelEvent) => {
            const canvas = canvasRef.current
            if (!canvas) return

            const rect = canvas.getBoundingClientRect()
            const mouseX = e.clientX - rect.left
            const mouseY = e.clientY - rect.top
            const area = getMouseArea(mouseX, mouseY, rect)

            const delta = e.deltaY < 0 ? 1.1 : 0.9

            switch (area) {
                case "x-axis":
                    // X축 영역: 시간축 줌인/아웃
                    e.preventDefault()
                    e.stopPropagation()
                    setChartState((prev) => ({
                        ...prev,
                        visibleCandleCount: Math.max(
                            prev.minVisibleCandles,
                            Math.min(prev.maxVisibleCandles, Math.round(prev.visibleCandleCount * delta)),
                        ),
                    }))
                    break

                case "y-axis":
                    // Y축 영역: 가격축 줌인/아웃
                    e.preventDefault()
                    e.stopPropagation()
                    setChartState((prev) => ({
                        ...prev,
                        priceRangeMultiplier: Math.max(0.1, Math.min(10, prev.priceRangeMultiplier * delta)),
                    }))
                    break

                case "chart":
                    // 그래프 영역: 시간대 조절 (과거/실시간 이동)
                    e.preventDefault()
                    e.stopPropagation()
                    const timeOffset = e.deltaY > 0 ? 5 : -5
                    setChartState((prev) => ({
                        ...prev,
                        offsetFromEnd: Math.max(0, Math.min(data.length - 1, prev.offsetFromEnd + timeOffset)),
                    }))
                    break

                case "chart-other":
                    // 차트 내부 기타 영역: 페이지 스크롤 허용
                    break

                default:
                    // 차트 외부: 페이지 스크롤 허용
                    break
            }
        }

        container.addEventListener("wheel", handleNativeWheel, { passive: false })

        return () => {
            container.removeEventListener("wheel", handleNativeWheel)
        }
    }, [getMouseArea, data.length])

    useEffect(() => {
        const canvas = canvasRef.current
        if (!canvas) return

        const ctx = canvas.getContext("2d")
        if (!ctx) return

        const visibleData = getVisibleData()
        if (visibleData.length === 0) return

        // Set canvas size
        const rect = canvas.getBoundingClientRect()
        canvas.width = rect.width * window.devicePixelRatio
        canvas.height = rect.height * window.devicePixelRatio
        ctx.scale(window.devicePixelRatio, window.devicePixelRatio)

        // Clear canvas
        ctx.clearRect(0, 0, rect.width, rect.height)

        // Calculate dimensions
        const padding = 40
        const chartWidth = rect.width - padding * 2
        const chartHeight = rect.height - padding * 2

        // Find min/max values for visible data
        const allPrices = visibleData.flatMap((d) => [d.high, d.low])

        // Include MA values in price range calculation
        overlays.forEach((overlay) => {
            if (overlay.visible && overlay.type === "MA") {
                const startIndex = data.length - chartState.offsetFromEnd - visibleData.length
                const maData = calculateMA(data, overlay.period)
                for (let i = 0; i < visibleData.length; i++) {
                    const maIndex = startIndex + i
                    if (maIndex >= 0 && maIndex < maData.length && !isNaN(maData[maIndex])) {
                        allPrices.push(maData[maIndex])
                    }
                }
            }
        })

        const minPrice = Math.min(...allPrices)
        const maxPrice = Math.max(...allPrices)
        const priceRange = (maxPrice - minPrice) * chartState.priceRangeMultiplier
        const priceCenter = (maxPrice + minPrice) / 2
        const adjustedMinPrice = priceCenter - priceRange / 2
        const adjustedMaxPrice = priceCenter + priceRange / 2

        // Draw area highlights based on current mouse position (subtle)
        if (currentArea === "x-axis") {
            ctx.fillStyle = "rgba(59, 130, 246, 0.05)"
            ctx.fillRect(padding, rect.height - padding, chartWidth, padding)
        } else if (currentArea === "y-axis") {
            ctx.fillStyle = "rgba(34, 197, 94, 0.05)"
            ctx.fillRect(0, padding, padding, chartHeight)
        } else if (currentArea === "chart") {
            ctx.fillStyle = "rgba(168, 85, 247, 0.02)"
            ctx.fillRect(padding, padding, chartWidth, chartHeight)
        }

        // Draw grid lines
        ctx.strokeStyle = "#e5e7eb"
        ctx.lineWidth = 1

        // Horizontal grid lines
        for (let i = 0; i <= 5; i++) {
            const y = padding + (chartHeight / 5) * i
            ctx.beginPath()
            ctx.moveTo(padding, y)
            ctx.lineTo(rect.width - padding, y)
            ctx.stroke()
        }

        // Vertical grid lines
        const timeStep = Math.max(1, Math.floor(visibleData.length / 10))
        for (let i = 0; i < visibleData.length; i += timeStep) {
            const x = padding + (i / visibleData.length) * chartWidth
            ctx.beginPath()
            ctx.moveTo(x, padding)
            ctx.lineTo(x, rect.height - padding)
            ctx.stroke()
        }

        // Draw area boundaries (very subtle)
        ctx.strokeStyle = "rgba(156, 163, 175, 0.2)"
        ctx.lineWidth = 1
        ctx.strokeRect(padding, padding, chartWidth, chartHeight)

        // Draw candlesticks
        const candleWidth = Math.max(1, (chartWidth / visibleData.length) * 0.8)
        const candleSpacing = chartWidth / visibleData.length

        visibleData.forEach((candle, index) => {
            const x = padding + index * candleSpacing + candleSpacing / 2
            const openY = padding + chartHeight - ((candle.open - adjustedMinPrice) / priceRange) * chartHeight
            const closeY = padding + chartHeight - ((candle.close - adjustedMinPrice) / priceRange) * chartHeight
            const highY = padding + chartHeight - ((candle.high - adjustedMinPrice) / priceRange) * chartHeight
            const lowY = padding + chartHeight - ((candle.low - adjustedMinPrice) / priceRange) * chartHeight

            const isGreen = candle.close > candle.open
            const color = isGreen ? "#10b981" : "#ef4444"

            // Draw wick
            ctx.strokeStyle = color
            ctx.lineWidth = 1
            ctx.beginPath()
            ctx.moveTo(x, highY)
            ctx.lineTo(x, lowY)
            ctx.stroke()

            // Draw body
            ctx.fillStyle = color
            const bodyTop = Math.min(openY, closeY)
            const bodyHeight = Math.abs(closeY - openY)
            ctx.fillRect(x - candleWidth / 2, bodyTop, candleWidth, bodyHeight || 1)
        })

        // Draw overlays (MA lines)
        overlays.forEach((overlay) => {
            if (!overlay.visible) return

            if (overlay.type === "MA") {
                const startIndex = data.length - chartState.offsetFromEnd - visibleData.length
                const maData = calculateMA(data, overlay.period)

                ctx.strokeStyle = overlay.color
                ctx.lineWidth = 2
                ctx.beginPath()

                let firstPoint = true
                visibleData.forEach((_, index) => {
                    const maIndex = startIndex + index
                    if (maIndex >= 0 && maIndex < maData.length && !isNaN(maData[maIndex])) {
                        const x = padding + index * candleSpacing + candleSpacing / 2
                        const y = padding + chartHeight - ((maData[maIndex] - adjustedMinPrice) / priceRange) * chartHeight

                        if (firstPoint) {
                            ctx.moveTo(x, y)
                            firstPoint = false
                        } else {
                            ctx.lineTo(x, y)
                        }
                    }
                })
                ctx.stroke()
            }
        })

        // Draw crosshair if hovering in chart area
        if (hoverInfo && currentArea === "chart") {
            ctx.strokeStyle = "#6b7280"
            ctx.lineWidth = 1
            ctx.setLineDash([5, 5])

            // Vertical line
            ctx.beginPath()
            ctx.moveTo(mousePosition.x, padding)
            ctx.lineTo(mousePosition.x, rect.height - padding)
            ctx.stroke()

            // Horizontal line
            ctx.beginPath()
            ctx.moveTo(padding, mousePosition.y)
            ctx.lineTo(rect.width - padding, mousePosition.y)
            ctx.stroke()

            ctx.setLineDash([])
        }

        // Draw price labels (Y축)
        ctx.fillStyle = "#6b7280"
        ctx.font = "12px sans-serif"
        ctx.textAlign = "right"

        for (let i = 0; i <= 5; i++) {
            const price = adjustedMinPrice + (priceRange / 5) * (5 - i)
            const y = padding + (chartHeight / 5) * i
            ctx.fillText(price.toLocaleString(), padding - 10, y + 4)
        }

        // Draw time labels (X축)
        ctx.textAlign = "center"
        for (let i = 0; i < visibleData.length; i += timeStep) {
            if (i < visibleData.length) {
                const x = padding + i * candleSpacing + candleSpacing / 2
                const time = new Date(visibleData[i].timestamp).toLocaleTimeString("ko-KR", {
                    hour: "2-digit",
                    minute: "2-digit",
                })
                ctx.fillText(time, x, rect.height - 10)
            }
        }

        // Draw chart info (top left, subtle)
        ctx.textAlign = "left"
        ctx.fillStyle = "#9ca3af"
        ctx.font = "12px sans-serif"
        ctx.fillText(`캔들 수: ${visibleData.length}`, padding, 20)

        if (chartState.offsetFromEnd > 0) {
            ctx.fillText(`과거 ${chartState.offsetFromEnd}개 이전`, padding + 120, 20)
        } else {
            ctx.fillText("실시간", padding + 120, 20)
        }
    }, [data, overlays, calculateMA, chartState, hoverInfo, mousePosition, currentArea])

    return (
        <div ref={containerRef} className="relative">
            <canvas
                ref={canvasRef}
                className="w-full h-96 cursor-crosshair"
                style={{
                    width: "100%",
                    height: "384px",
                }}
                onMouseDown={handleMouseDown}
                onMouseMove={handleMouseMove}
                onMouseUp={handleMouseUp}
                onMouseLeave={handleMouseLeave}
            />

            {/* Price Info Tooltip */}
            {hoverInfo && currentArea === "chart" && (
                <div
                    className="absolute bg-white border rounded-lg shadow-lg p-3 pointer-events-none z-10"
                    style={{
                        left: Math.min(mousePosition.x + 10, window.innerWidth - 300),
                        top: Math.max(mousePosition.y - 10, 10),
                    }}
                >
                    <div className="text-sm space-y-1">
                        <div className="font-medium">가격: {hoverInfo.price.toLocaleString()}</div>
                        <div>시간: {hoverInfo.time}</div>
                        <div>고가: {hoverInfo.high.toLocaleString()}</div>
                        <div>저가: {hoverInfo.low.toLocaleString()}</div>
                        <div>시가: {hoverInfo.open.toLocaleString()}</div>
                        <div>종가: {hoverInfo.close.toLocaleString()}</div>
                        <div>거래량: {hoverInfo.volume.toFixed(3)}</div>
                        {Object.entries(hoverInfo.maValues).map(([key, value]) => (
                            <div key={key} className="text-blue-600">
                                {key}: {value.toLocaleString()}
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    )
}
