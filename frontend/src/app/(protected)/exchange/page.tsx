"use client";

import { useEffect, useRef, useState } from "react";
import { PriceSummary } from "@/components/exchange/PriceSummary";
import { CoinList } from "@/components/exchange/CoinList";
import { ExchangeDTO, CandleInterval } from "@/lib/types/exchange/type";
import { TradeForm } from "@/components/exchange/TradeForm";
import { CandleChart } from "@/components/exchange/CandleChart/CandleChart";
import { exchangeApi } from "@/lib/api/exchange";

export default function TradingPage() {
    const [selectedCoin, setSelectedCoin] = useState<ExchangeDTO | null>(null);
    const [loadStatus, setLoadStatus] = useState<"loading" | "empty" | "success">("loading");
    const [allCoins, setAllCoins] = useState<ExchangeDTO[]>([]);
    const leftRef = useRef<HTMLDivElement>(null);
    const [leftHeight, setLeftHeight] = useState<number>(0);
    const [isWide, setIsWide] = useState<boolean>(typeof window !== "undefined" ? window.innerWidth > 1480 : true);

    useEffect(() => {
        const fetchInitialCoins = async () => {
            try {
                const data = await exchangeApi.getLatest();
                if (data.length > 0) {
                    setAllCoins(data);
                    // 데이터가 있으면 BTC를 기본 선택 코인으로 설정
                    const btc = data.find((c) => c.market === "KRW-BTC") || data[0];
                    setSelectedCoin(btc);
                    setLoadStatus("success");
                } else {
                    setLoadStatus("empty");
                }
            } catch (error) {
                console.error("초기 코인 데이터 로딩 실패:", error);
                setLoadStatus("empty"); // 에러 발생 시 '코인 없음'으로 처리
            }
        };

        fetchInitialCoins();
    }, []);

    const updateLayout = () => {
        setIsWide(window.innerWidth > 1480);
    };

    useEffect(() => {
        const update = () => {
            updateLayout();
            if (leftRef.current) {
                setLeftHeight(leftRef.current.offsetHeight);
            }
        };

        update();
        window.addEventListener("resize", update);
        return () => window.removeEventListener("resize", update);
    }, [selectedCoin]);


    if (loadStatus === "loading") {
        return (
            <div className="flex items-center justify-center h-screen text-lg text-gray-600">
                데이터를 불러오는 중입니다...
            </div>
        );
    }

    if (loadStatus === "empty") {
        return (
            <div className="flex items-center justify-center h-screen text-lg text-gray-500">
                등록된 코인이 없습니다. 관리자에게 문의하세요.
            </div>
        );
    }

    return (
        <div
            className={`flex gap-4 p-4 overflow-x-hidden justify-between ${
                isWide ? "flex-row" : "flex-col"
            }`}
        >
            {/* 차트 영역 */}
            <div
                ref={leftRef}
                className="flex flex-col flex-1 min-h-[600px] gap-4 justify-between w-full min-w-[300px] sm:min-w-[500px] [@media(max-width:2240px)]:min-w-[800px] lg:min-w-[1400px]"
            >
                {selectedCoin && (
                    <>
                        <PriceSummary coin={selectedCoin} />
                        <CandleChart market={selectedCoin.market} intval={CandleInterval.HOUR_1} />
                        <TradeForm selectedCoin={selectedCoin} />
                    </>
                )}
            </div>

            {/* 코인 리스트 */}
            <div
                className="w-full min-w-[280px] flex-1"
                style={selectedCoin ? { height: leftHeight } : undefined}
            >
                <CoinList
                    initialCoins={allCoins}
                    onSelect={(market, coin) => setSelectedCoin(coin)}
                />
            </div>
        </div>
    );
}
