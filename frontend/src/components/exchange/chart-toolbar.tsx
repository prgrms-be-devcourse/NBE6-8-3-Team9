"use client"

import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { ZoomIn, ZoomOut, RotateCcw, X, Plus } from "lucide-react"
import { useState } from "react"
import {Overlay} from "@/lib/types/exchange/type";

interface ChartToolbarProps {
    selectedTimeframe: string
    onTimeframeChange: (timeframe: string) => void
    onZoomIn: () => void
    onZoomOut: () => void
    onReset: () => void
    overlays: Overlay[]
    onAddOverlay: (period: number) => void
    onRemoveOverlay: (id: string) => void
    onToggleOverlay: (id: string) => void
}

const timeframes = [
    { value: "1s", label: "1초" },
    { value: "1m", label: "1분" },
    { value: "30m", label: "30분" },
    { value: "1h", label: "1시간" },
    { value: "1d", label: "1일" },
    { value: "1w", label: "1주" },
    { value: "1M", label: "1달" },
    { value: "1y", label: "1년" },
]

export default function ChartToolbar({
                                         selectedTimeframe,
                                         onTimeframeChange,
                                         onZoomIn,
                                         onZoomOut,
                                         onReset,
                                         overlays,
                                         onAddOverlay,
                                         onRemoveOverlay,
                                         onToggleOverlay,
                                     }: ChartToolbarProps) {
    const [showOverlayPanel, setShowOverlayPanel] = useState(false)
    const [newOverlayPeriod, setNewOverlayPeriod] = useState("20")

    const handleAddOverlay = () => {
        const period = Number.parseInt(newOverlayPeriod)
        if (period > 0) {
            onAddOverlay(period)
            setNewOverlayPeriod("")
        }
    }

    const handleReset = () => {
        onReset() // 기존 onReset 호출
        // 추가로 상태 초기화
        onTimeframeChange("1m") // 1분으로 초기화
        // MA 15, 50으로 초기화는 부모 컴포넌트에서 처리
    }

    const handleZoomIn = () => {
        onZoomIn() // X축 줌인 동작
    }

    const handleZoomOut = () => {
        onZoomOut() // X축 줌아웃 동작
    }

    return (
        <div className="relative">
            <div className="flex items-center justify-between p-2 border-b">
                <div className="flex items-center space-x-2">
                    <Button variant="outline" size="sm" onClick={() => setShowOverlayPanel(!showOverlayPanel)}>
                        차트설정
                    </Button>
                </div>

                <div className="flex items-center space-x-2">
                    <Select value={selectedTimeframe} onValueChange={onTimeframeChange}>
                        <SelectTrigger className="w-24">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {timeframes.map((tf) => (
                                <SelectItem key={tf.value} value={tf.value}>
                                    {tf.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>

                    <div className="flex items-center space-x-1">
                        <Button variant="outline" size="sm" onClick={handleZoomOut}>
                            <ZoomOut className="w-4 h-4" />
                        </Button>
                        <Button variant="outline" size="sm" onClick={handleZoomIn}>
                            <ZoomIn className="w-4 h-4" />
                        </Button>
                        <Button variant="outline" size="sm" onClick={handleReset}>
                            <RotateCcw className="w-4 h-4" />
                        </Button>
                    </div>
                </div>
            </div>

            {/* Overlay Panel */}
            {showOverlayPanel && (
                <div className="absolute top-full left-2 z-10 mt-2">
                    <Card className="w-64">
                        <CardContent className="p-4">
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="font-medium">오버레이</h3>
                                <Button variant="ghost" size="sm" onClick={() => setShowOverlayPanel(false)}>
                                    <X className="w-4 h-4" />
                                </Button>
                            </div>

                            <div className="space-y-2 mb-4">
                                {overlays.map((overlay) => (
                                    <div key={overlay.id} className="flex items-center justify-between p-2 border rounded">
                                        <div className="flex items-center space-x-2">
                                            <div
                                                className="w-3 h-3 rounded"
                                                style={{ backgroundColor: overlay.visible ? overlay.color : "#d1d5db" }}
                                            />
                                            <span className="text-sm">
                        {overlay.type} ({overlay.period})
                      </span>
                                        </div>
                                        <div className="flex items-center space-x-1">
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                onClick={() => onToggleOverlay(overlay.id)}
                                                className="p-1 h-6 w-6"
                                            >
                                                {overlay.visible ? "👁️" : "👁️‍🗨️"}
                                            </Button>
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                onClick={() => onRemoveOverlay(overlay.id)}
                                                className="p-1 h-6 w-6"
                                            >
                                                <X className="w-3 h-3" />
                                            </Button>
                                        </div>
                                    </div>
                                ))}
                            </div>

                            <div className="space-y-2">
                                <div className="flex items-center space-x-2">
                                    <Input
                                        placeholder="기간"
                                        value={newOverlayPeriod}
                                        onChange={(e) => setNewOverlayPeriod(e.target.value)}
                                        className="flex-1"
                                        type="number"
                                    />
                                    <Button size="sm" onClick={handleAddOverlay}>
                                        <Plus className="w-4 h-4" />
                                    </Button>
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                </div>
            )}
        </div>
    )
}
