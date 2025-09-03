"use client";

import { motion } from "framer-motion";
import { fadeInUp, stagger } from "@/lib/motion";
import { PageShell } from "@/components/layout/page-shell";
import { useEffect, useState } from "react";
import { apiCall } from "@/lib/api/client";
import { walletApi } from "@/lib/api/wallet";
import type { WalletDto } from "@/lib/types/wallet";
import { FaWallet, FaCoins, FaPiggyBank } from "react-icons/fa6";

const statStyles = [
    {
        bg: "bg-gradient-to-r from-amber-400 to-yellow-300",
        icon: <FaWallet className="text-3xl text-white mb-2" />,
        text: "text-amber-900"
    },
    {
        bg: "bg-gradient-to-r from-emerald-400 to-green-300",
        icon: <FaCoins className="text-3xl text-white mb-2" />,
        text: "text-emerald-900"
    },
    {
        bg: "bg-gradient-to-r from-sky-400 to-blue-300",
        icon: <FaPiggyBank className="text-3xl text-white mb-2" />,
        text: "text-sky-900"
    }
];

export function StatsStrip() {
    const [walletData, setWalletData] = useState<WalletDto | null>(null);
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const loadUserAndWallet = async () => {
            try {
                const userResponse = await apiCall<{ result: { id: number } }>('/api/v1/users/me');
                if (userResponse?.result?.id) {
                    setIsLoggedIn(true);
                    const id = userResponse.result.id;
                    try {
                        const walletResponse = await walletApi.getWallet(id);
                        setWalletData(walletResponse);
                    } catch {
                        setWalletData(null);
                    }
                } else {
                    setIsLoggedIn(false);
                }
            } catch {
                setIsLoggedIn(false);
            } finally {
                setIsLoading(false);
            }
        };

        loadUserAndWallet();
    }, []);

    if (isLoading || !isLoggedIn) {
        return null;
    }

    const totalInvestment = walletData?.coinAmounts?.reduce((acc, coin) => acc + coin.totalAmount, 0) || 0;
    const currentBalance = walletData?.balance || 0;
    const totalAssets = totalInvestment + currentBalance;

    const stats = [
        { label: "총 자산", value: `₩ ${totalAssets.toLocaleString()}` },
        { label: "총 투자금", value: `₩ ${totalInvestment.toLocaleString()}` },
        { label: "투자 가능한 금액", value: `₩ ${currentBalance.toLocaleString()}` },
    ];

    return (
        <section className="py-12">
            <PageShell maxW="max-w-[80vw]" padded>
                <motion.div
                    variants={stagger(0.1)}
                    initial="hidden"
                    whileInView="show"
                    viewport={{ once: true, amount: 0.2 }}
                    className="grid gap-6 md:grid-cols-3"
                >
                    {stats.map((stat, i) => (
                        <motion.div
                            key={stat.label}
                            variants={fadeInUp}
                            className={`rounded-xl shadow-lg p-8 flex flex-col items-center ${statStyles[i].bg}`}
                        >
                            {statStyles[i].icon}
                            <div className="text-base font-semibold text-white mb-1">{stat.label}</div>
                            <div className={`text-3xl font-extrabold ${statStyles[i].text}`}>{stat.value}</div>
                        </motion.div>
                    ))}
                </motion.div>
            </PageShell>
        </section>
    );
}