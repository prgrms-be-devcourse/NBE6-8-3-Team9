"use client"

import { useEffect, useRef, useState, useCallback } from "react"
import {CandleData} from "@/lib/types/exchange/type";

interface UpbitWebSocketOptions {
    symbols: string[]
    timeframe: string
    onCandleUpdate: (data: CandleData) => void
    onError?: (error: string) => void
}

// Upbit WebSocket timeframe mapping
const timeframeMap: Record<string, string> = {
    "1s": "1s",
    "1m": "1m",
    "30m": "30m",
    "1h": "1h",
    "1d": "1d",
    "1w": "1w",
    "1M": "1M",
    "1y": "1y",
}

export function useUpbitWebSocket({ symbols, timeframe, onCandleUpdate, onError }: UpbitWebSocketOptions) {
    const [isConnected, setIsConnected] = useState(false)
    const [connectionStatus, setConnectionStatus] = useState<"connecting" | "connected" | "disconnected" | "error">(
        "error", // Start with error since we can't connect in browser
    )
    const wsRef = useRef<WebSocket | null>(null)
    const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null)
    const maxReconnectAttempts = 3
    const reconnectAttempts = useRef(0)

    // Stable error handler
    const handleError = useCallback(
        (errorMessage: string) => {
            console.log("WebSocket:", errorMessage)
            setConnectionStatus("error")
            onError?.(errorMessage)
        },
        [onError],
    )

    const connect = useCallback(() => {
        // Skip connection in browser environment due to CORS restrictions
        if (typeof window !== "undefined") {
            console.log("WebSocket connection skipped in browser environment due to CORS restrictions")
            handleError("WebSocket connection not available in browser environment. Using mock data.")
            return
        }

        try {
            setConnectionStatus("connecting")
            wsRef.current = new WebSocket("wss://api.upbit.com/websocket/v1")

            wsRef.current.onopen = () => {
                console.log("WebSocket connected to Upbit")
                setIsConnected(true)
                setConnectionStatus("connected")
                reconnectAttempts.current = 0

                // Subscribe to candle data based on timeframe
                const subscribeMessage = [
                    {
                        ticket: "upbit-chart-" + Date.now(),
                    },
                    {
                        type: "candle",
                        codes: symbols,
                        unit: timeframeMap[timeframe] || "1m",
                    },
                ]

                if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
                    wsRef.current.send(JSON.stringify(subscribeMessage))
                }
            }

            wsRef.current.onmessage = (event) => {
                try {
                    // Handle both text and binary data
                    if (event.data instanceof Blob) {
                        const reader = new FileReader()
                        reader.onload = () => {
                            try {
                                const data = JSON.parse(reader.result as string)
                                processUpbitData(data)
                            } catch (parseError) {
                                console.error("Failed to parse binary WebSocket message:", parseError)
                            }
                        }
                        reader.readAsText(event.data)
                    } else {
                        const data = JSON.parse(event.data)
                        processUpbitData(data)
                    }
                } catch (error) {
                    console.error("Failed to process WebSocket message:", error)
                }
            }

            wsRef.current.onerror = (error) => {
                console.error("WebSocket connection error:", error)
                handleError("WebSocket connection failed")
            }

            wsRef.current.onclose = (event) => {
                console.log("WebSocket connection closed:", event.code, event.reason)
                setIsConnected(false)
                setConnectionStatus("disconnected")

                // Attempt to reconnect if not manually closed
                if (event.code !== 1000 && reconnectAttempts.current < maxReconnectAttempts) {
                    reconnectAttempts.current++
                    console.log(`Attempting to reconnect... (${reconnectAttempts.current}/${maxReconnectAttempts})`)
                    reconnectTimeoutRef.current = setTimeout(connect, 3000)
                } else {
                    handleError("WebSocket connection lost. Using mock data.")
                }
            }
        } catch (error) {
            console.error("Failed to create WebSocket connection:", error)
            handleError("Failed to establish WebSocket connection")
        }
    }, [symbols, timeframe, onCandleUpdate, handleError])

    const processUpbitData = (data: any) => {
        // Convert Upbit data format to our CandleData format
        if (data.type === "candle" || data.cd) {
            const candleData: CandleData = {
                timestamp: new Date(data.candle_date_time_kst || data.ct).getTime(),
                open: data.opening_price || data.op,
                high: data.high_price || data.hp,
                low: data.low_price || data.lp,
                close: data.trade_price || data.tp,
                volume: data.candle_acc_trade_volume || data.tv,
            }
            onCandleUpdate(candleData)
        }
    }

    useEffect(() => {
        connect()

        return () => {
            if (reconnectTimeoutRef.current) {
                clearTimeout(reconnectTimeoutRef.current)
            }
            if (wsRef.current) {
                wsRef.current.close(1000, "Component unmounting")
            }
        }
    }, [connect]) // Only depend on stable connect

    const disconnect = useCallback(() => {
        if (reconnectTimeoutRef.current) {
            clearTimeout(reconnectTimeoutRef.current)
        }
        if (wsRef.current) {
            wsRef.current.close(1000, "Manual disconnect")
        }
        setIsConnected(false)
        setConnectionStatus("disconnected")
    }, [])

    return { isConnected, connectionStatus, disconnect }
}
