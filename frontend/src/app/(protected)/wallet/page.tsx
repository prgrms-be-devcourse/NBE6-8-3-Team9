"use client";

import { motion } from "framer-motion";
import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { walletApi } from "@/lib/api/wallet";
import type { WalletDto } from "@/lib/types/wallet";

const fadeInUp = {
    hidden: { opacity: 0, y: 16 },
    show: { opacity: 1, y: 0, transition: { duration: 0.4 } },
};

const stagger = (delay = 0.1) => ({
    hidden: {},
    show: {
        transition: {
            staggerChildren: delay,
        },
    },
});

export default function WalletPage() {
    const [walletData, setWalletData] = useState<WalletDto | null>(null);
    const [chargeAmount, setChargeAmount] = useState("");
    const [isCharging, setIsCharging] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const userId = 1; // 실제로는 auth에서 가져와야 함

    // 지갑 정보 조회 함수
    const fetchWallet = async () => {
        try {
            setIsLoading(true);
            const data = await walletApi.getWallet(userId);
            if (data) {
                setWalletData(data);
            }
        } catch (error) {
            console.error('지갑 정보 조회 실패:', error);
        } finally {
            setIsLoading(false);
        }
    };

    // 충전 함수
    const handleCharge = async () => {
        if (!chargeAmount) return;

        setIsCharging(true);
        try {
            const result = await walletApi.charge(userId, parseFloat(chargeAmount));

            if (result !== null) {
                // 충전 후 지갑 정보 다시 조회
                await fetchWallet();
                setChargeAmount("");
            }
        } catch (error) {
            console.error('충전 실패:', error);
        } finally {
            setIsCharging(false);
        }
    };

    // 컴포넌트 마운트 시 지갑 정보 조회
    useEffect(() => {
        fetchWallet();
    }, []);

    // 총 자산 계산
    const totalValue = walletData?.coinAmounts.reduce((acc, coin) => acc + coin.totalAmount, 0) || 0;

    if (isLoading) {
        return (
            <div className="container py-8">
                <div>지갑 정보를 불러오는 중...</div>
            </div>
        );
    }

    if (!walletData) {
        return (
            <div className="container py-8">
                <div>지갑 정보를 불러올 수 없습니다.</div>
            </div>
        );
    }

    return (
        <motion.div
            className="container py-8 space-y-6"
            variants={stagger(0.1)}
            initial="hidden"
            animate="show"
        >
            <motion.h1
                variants={fadeInUp}
                className="text-2xl font-bold"
            >
                지갑
            </motion.h1>

            <motion.div
                variants={fadeInUp}
                className="border rounded-lg p-6"
            >
                <div className="text-sm text-muted-foreground">총 자산</div>
                <div className="text-2xl font-semibold">${totalValue.toLocaleString()}</div>
                <div className="text-sm text-muted-foreground mt-2">
                    현재 잔액: {walletData.balance.toLocaleString()}원
                </div>
                <div className="text-xs text-muted-foreground mt-1">
                    지갑 주소: {walletData.address}
                </div>
            </motion.div>

            <motion.div
                variants={fadeInUp}
                className="border rounded-lg p-6"
            >
                <div className="flex justify-between items-center mb-4">
                    <h2 className="text-lg font-semibold">지갑 충전</h2>
                </div>
                <div className="space-y-4">
                    <div>
                        <label className="text-sm font-medium">충전 금액</label>
                        <Input
                            type="number"
                            placeholder="충전할 금액을 입력하세요"
                            value={chargeAmount}
                            onChange={(e) => setChargeAmount(e.target.value)}
                        />
                    </div>
                    <Button
                        onClick={handleCharge}
                        disabled={isCharging || !chargeAmount}
                        className="w-full"
                    >
                        {isCharging ? "충전 중..." : "충전하기"}
                    </Button>
                </div>
            </motion.div>

            <motion.div
                variants={stagger(0.05)}
                className="space-y-2"
            >
                <h2 className="text-lg font-semibold mb-4">보유 코인</h2>
                {walletData.coinAmounts.length === 0 ? (
                    <div className="text-center text-muted-foreground py-8">
                        보유한 코인이 없습니다.
                    </div>
                ) : (
                    walletData.coinAmounts.map((coin) => (
                        <motion.div
                            key={coin.coinId}
                            variants={fadeInUp}
                            className="flex justify-between items-center border rounded-md p-4 transition hover:shadow-md hover:scale-[1.01]"
                        >
                            <div>
                                <div className="font-medium">{coin.coinSymbol}</div>
                                <div className="text-sm text-muted-foreground">
                                    {coin.coinName}
                                </div>
                                <div className="text-sm text-muted-foreground">
                                    보유량: {coin.quantity} {coin.coinSymbol}
                                </div>
                            </div>
                            <div className="text-right">
                                <div className="font-medium">
                                    ${coin.totalAmount.toLocaleString()}
                                </div>
                                <div className="text-sm text-muted-foreground">
                                    총 투자금액
                                </div>
                            </div>
                        </motion.div>
                    ))
                )}
            </motion.div>
        </motion.div>
    );
}
