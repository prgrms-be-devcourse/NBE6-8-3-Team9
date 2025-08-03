"use client"

import { useEffect, useRef, useState } from "react"

interface UseWebSocketOptions {
    url: string
    onMessage?: (data: any) => void
    onError?: (error: Event) => void
    onOpen?: () => void
    onClose?: () => void
}

export function useWebSocket({ url, onMessage, onError, onOpen, onClose }: UseWebSocketOptions) {
    const [isConnected, setIsConnected] = useState(false)
    const wsRef = useRef<WebSocket | null>(null)

    useEffect(() => {
        const connect = () => {
            try {
                wsRef.current = new WebSocket(url)

                wsRef.current.onopen = () => {
                    setIsConnected(true)
                    onOpen?.()
                }

                wsRef.current.onmessage = (event) => {
                    try {
                        const data = JSON.parse(event.data)
                        onMessage?.(data)
                    } catch (error) {
                        console.error("Failed to parse WebSocket message:", error)
                    }
                }

                wsRef.current.onerror = (error) => {
                    console.error("WebSocket error:", error)
                    onError?.(error)
                }

                wsRef.current.onclose = () => {
                    setIsConnected(false)
                    onClose?.()

                    // Reconnect after 3 seconds
                    setTimeout(connect, 3000)
                }
            } catch (error) {
                console.error("Failed to connect to WebSocket:", error)
                setTimeout(connect, 3000)
            }
        }

        connect()

        return () => {
            if (wsRef.current) {
                wsRef.current.close()
            }
        }
    }, [url, onMessage, onError, onOpen, onClose])

    const sendMessage = (message: any) => {
        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
            wsRef.current.send(JSON.stringify(message))
        }
    }

    return { isConnected, sendMessage }
}
