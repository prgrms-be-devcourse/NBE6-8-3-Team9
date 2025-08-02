"use client"

import { Badge } from "@/components/ui/badge"

interface ConnectionStatusProps {
    status: "connecting" | "connected" | "disconnected" | "error"
    isConnected: boolean
}

export default function ConnectionStatus({ status, isConnected }: ConnectionStatusProps) {
    const getStatusConfig = () => {
        switch (status) {
            case "connecting":
                return {
                    variant: "secondary" as const,
                    text: "연결 중...",
                    className: "bg-yellow-100 text-yellow-800",
                }
            case "connected":
                return {
                    variant: "default" as const,
                    text: "실시간 연결됨",
                    className: "bg-green-100 text-green-800",
                }
            case "error":
                return {
                    variant: "secondary" as const,
                    text: "Demo Mode (Mock Data)",
                    className: "bg-blue-100 text-blue-800",
                }
            default:
                return {
                    variant: "outline" as const,
                    text: "연결 끊김",
                    className: "bg-gray-100 text-gray-800",
                }
        }
    }

    const config = getStatusConfig()

    return (
        <Badge variant={config.variant} className={config.className}>
            {config.text}
        </Badge>
    )
}
