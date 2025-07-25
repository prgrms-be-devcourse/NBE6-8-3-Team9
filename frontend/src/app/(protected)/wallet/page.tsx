"use client";

import { motion } from "framer-motion";

const mock = [
    { symbol: "BTC", balance: 0.22, valueUSD: 20000 },
    { symbol: "ETH", balance: 3.12, valueUSD: 9000 },
    { symbol: "USDT", balance: 1560, valueUSD: 1560 },
];

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
    const total = mock.reduce((acc, c) => acc + c.valueUSD, 0);

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
                <div className="text-2xl font-semibold">${total.toLocaleString()}</div>
            </motion.div>

            <motion.div
                variants={stagger(0.05)}
                className="space-y-2"
            >
                {mock.map((c) => (
                    <motion.div
                        key={c.symbol}
                        variants={fadeInUp}
                        className="flex justify-between border rounded-md p-4 transition hover:shadow-md hover:scale-[1.01]"
                    >
                        <div>
                            <div className="font-medium">{c.symbol}</div>
                            <div className="text-sm text-muted-foreground">
                                {c.balance} {c.symbol}
                            </div>
                        </div>
                        <div className="text-sm text-muted-foreground">
                            ${c.valueUSD.toLocaleString()}
                        </div>
                    </motion.div>
                ))}
            </motion.div>
        </motion.div>
    );
}
