"use client";

import { motion } from "framer-motion";
import { fadeInUp, stagger } from "@/lib/motion";
import { PageShell } from "@/components/layout/page-shell";

export function StatsStrip() {
    const stats = [
        { label: "총 자산", value: "₩ 7,265,000" },
        { label: "총 투자금", value: "₩ 3,671,000" },
        { label: "투자 가능한 금액", value: "₩ 156,000" },
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
                    {stats.map((s) => (
                        <motion.div
                            variants={fadeInUp}
                            key={s.label}
                            className="border rounded-lg p-6 bg-white shadow-sm transition hover:shadow-md hover:scale-[1.01]"
                        >
                            <div className="text-sm text-muted-foreground">{s.label}</div>
                            <div className="mt-2 text-2xl font-semibold">{s.value}</div>
                        </motion.div>
                    ))}
                </motion.div>
            </PageShell>
        </section>
    );
}