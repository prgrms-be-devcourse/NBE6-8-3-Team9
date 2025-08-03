"use client"

import { TrendingUp, TrendingDown } from "lucide-react"
import {CoinInfo} from "@/lib/types/exchange/type";

interface PriceHeaderProps {
    coinInfo: CoinInfo
}

export default function PriceHeader({ coinInfo }: PriceHeaderProps) {
    const isPositive = coinInfo.changePercent >= 0

    return (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            {/* Current Price */}
            <div>
                <div className="text-3xl font-bold text-gray-900">
                    {coinInfo.price.toLocaleString()}
                    <span className="text-sm font-normal text-gray-500 ml-2">KRW</span>
                </div>
                <div className={`flex items-center mt-1 ${isPositive ? "text-red-600" : "text-blue-600"}`}>
                    {isPositive ? <TrendingUp className="w-4 h-4 mr-1" /> : <TrendingDown className="w-4 h-4 mr-1" />}
                    <span className="font-medium">
            {isPositive ? "+" : ""}
                        {coinInfo.changePercent.toFixed(2)}%
          </span>
                    <span className="ml-2">
            {isPositive ? "▲" : "▼"} {Math.abs(coinInfo.change).toLocaleString()}
          </span>
                </div>
            </div>

            {/* High/Low */}
            <div className="space-y-2">
                <div className="flex justify-between">
                    <span className="text-gray-600">고가</span>
                    <span className="font-medium text-red-600">{(coinInfo.price * 1.02).toLocaleString()}</span>
                </div>
                <div className="flex justify-between">
                    <span className="text-gray-600">저가</span>
                    <span className="font-medium text-blue-600">{(coinInfo.price * 0.98).toLocaleString()}</span>
                </div>
            </div>

            {/* Market Info */}
            <div className="space-y-2">
                <div className="flex justify-between">
                    <span className="text-gray-600">시가</span>
                    <span className="font-medium">{(coinInfo.price * 0.995).toLocaleString()}</span>
                </div>
                <div className="flex justify-between">
                    <span className="text-gray-600">전일가</span>
                    <span className="font-medium">{(coinInfo.price - coinInfo.change).toLocaleString()}</span>
                </div>
            </div>

            {/* Volume */}
            <div className="space-y-2">
                <div className="flex justify-between">
                    <span className="text-gray-600">거래량</span>
                    <span className="font-medium">{coinInfo.volume.toFixed(3)}</span>
                </div>
                <div className="flex justify-between">
                    <span className="text-gray-600">거래대금</span>
                    <span className="font-medium">{((coinInfo.volume * coinInfo.price) / 1000000).toFixed(0)}백만</span>
                </div>
            </div>
        </div>
    )
}
