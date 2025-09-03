"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { useState, useEffect, useCallback, useRef } from "react";
import { cn } from "@/lib/utils";
import { PageShell } from "@/components/layout/page-shell";
import { apiCall } from "@/lib/api/client";
import { FaUserPlus } from "react-icons/fa6";

type HeroProps = {
    title?: string;
    subtitle?: string;
    primaryCta?: { href: string; label: string };
    secondaryCta?: { href: string; label: string };
    className?: string;
    innerClassName?: string;
};

const fadeInUp = {
    hidden: { opacity: 0, y: 16 },
    show: { opacity: 1, y: 0, transition: { duration: 0.4 } },
};

const stagger = (delay = 0.08) => ({
    hidden: {},
    show: { transition: { staggerChildren: delay } },
});

export function Hero({
                         title = "Back9 Coin",
                         subtitle = "투자의 기준을 바꾸다. 실시간 지갑, 거래 내역,\n관리자 전용 코인 등록까지.",
                         primaryCta = { href: "/exchange", label: "대시보드 보러가기" },
                         secondaryCta = { href: "/register", label: "회원가입" },
                         className,
                         innerClassName,
                     }: HeroProps) {
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [mounted, setMounted] = useState(false);
    const fetchingRef = useRef(false);

    const checkLoginStatus = useCallback(async () => {
        if (fetchingRef.current) return;
        fetchingRef.current = true;
        try {
            await apiCall('/api/v1/users/me');
            setIsLoggedIn(true);
        } catch {
            setIsLoggedIn(false);
        } finally {
            fetchingRef.current = false;
        }
    }, []);

    useEffect(() => {
        setMounted(true);
        checkLoginStatus();
        const onFocus = () => void checkLoginStatus();
        window.addEventListener("focus", onFocus);
        return () => window.removeEventListener("focus", onFocus);
    }, [checkLoginStatus]);

    return (
        <section className={cn("relative overflow-hidden w-full", className)}>
            {/* 배경 SVG 패턴 */}
            <div className="pointer-events-none absolute inset-0 -z-10 bg-gradient-to-b from-orange-50 to-white" />
            <svg
                className="absolute left-1/2 top-0 -translate-x-1/2 -z-10 opacity-30"
                width="800"
                height="400"
                viewBox="0 0 800 400"
                fill="none"
            >
                <circle cx="400" cy="200" r="180" fill="url(#hero-gradient)" />
                <defs>
                    <radialGradient id="hero-gradient" cx="0" cy="0" r="1" gradientTransform="translate(400 200) scale(180)" gradientUnits="userSpaceOnUse">
                        <stop stopColor="#fbbf24" />
                        <stop offset="1" stopColor="#fff" stopOpacity="0" />
                    </radialGradient>
                </defs>
            </svg>

            <PageShell
                maxW="max-w-[80vw]"
                padded
                innerClassName={cn(
                    "min-h-[60vh] flex flex-col items-center justify-center text-center space-y-8",
                    innerClassName
                )}
            >
                <motion.div variants={stagger(0.1)} initial="hidden" animate="show">
                    <motion.div
                        variants={fadeInUp}
                        className="relative w-full max-w-2xl mx-auto flex items-center justify-between gap-3"
                        style={{ minHeight: "5rem" }}
                    >
                        {/* 왼쪽 영역 */}
                        <div className="flex-1 flex justify-end">
                            <img
                                src="/images/back9-coin-logo.PNG"
                                alt="BACK9 Coin Logo"
                                className="w-14 h-14 md:w-16 md:h-16 object-contain drop-shadow-lg"
                            />
                        </div>

                        {/* 타이틀 */}
                        <h1 className="text-4xl md:text-5xl font-extrabold text-amber-600
        drop-shadow-[0_2px_8px_rgba(251,191,36,0.2)] text-center flex-shrink-0">
                            {title}
                        </h1>

                        {/* 오른쪽 영역 */}
                        <div className="flex-1 flex justify-start">
                            <motion.img
                                src="/images/rocket.png"
                                alt="Rocket"
                                className="w-20 h-20 md:w-24 md:h-24 object-contain drop-shadow-lg"
                                animate={{ y: [0, -20, 0] }}
                                transition={{
                                    duration: 1.2,
                                    repeat: Infinity,
                                    repeatType: "loop",
                                    ease: "easeInOut",
                                }}
                            />
                        </div>
                    </motion.div>

                    <motion.p
                        variants={fadeInUp}
                        className="text-lg md:text-xl text-gray-700 max-w-xl mx-auto mt-4 font-medium whitespace-pre-line"
                    >
                        {subtitle}
                    </motion.p>

                    <motion.div variants={fadeInUp} className="mt-8 flex justify-center gap-4">
                        <Link
                            href={primaryCta.href}
                            className="inline-flex items-center gap-2 px-6 py-3 rounded-lg bg-amber-500 text-white font-bold text-lg shadow-lg transition
                                hover:bg-amber-600 hover:scale-105 active:scale-95"
                        >
                            {primaryCta.label}
                        </Link>
                        {mounted && !isLoggedIn && (
                            <Link
                                href={secondaryCta.href}
                                className="inline-flex items-center gap-2 px-6 py-3 rounded-lg border-2 border-amber-400 text-amber-600 font-bold text-lg bg-white shadow transition
                                    hover:bg-amber-50 hover:scale-105 active:scale-95"
                            >
                                <FaUserPlus className="text-amber-400" />
                                {secondaryCta.label}
                            </Link>
                        )}
                    </motion.div>

                    {/* 하단 안내/포인트 */}
                    <motion.div
                        variants={fadeInUp}
                        className="mt-10 flex flex-col md:flex-row items-center justify-center gap-6 text-base text-gray-600"
                    >
                        <span className="font-medium tracking-wide text-lg text-gray-700">
                            쉽고 안전한 모의 투자 서비스
                        </span>
                        <span className="hidden md:inline text-gray-300 text-lg font-bold px-2">·</span>
                        <span className="font-medium tracking-wide text-lg text-gray-700">
                            지갑 & 거래 내역 제공
                        </span>
                        <span className="hidden md:inline text-gray-300 text-lg font-bold px-2">·</span>
                        <span className="font-medium tracking-wide text-lg text-gray-700">
                            관리자 코인 등록 지원
                        </span>
                    </motion.div>
                </motion.div>
            </PageShell>
        </section>
    );
}