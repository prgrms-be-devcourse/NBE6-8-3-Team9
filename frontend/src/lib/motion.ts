export const fadeInUp = {
    hidden: { opacity: 0, y: 16 },
    show:   { opacity: 1, y: 0, transition: { duration: 0.35, ease: "easeOut" } },
} as const;

export const stagger = (staggerChildren = 0.08, delayChildren = 0) => ({
    hidden: { opacity: 1 },
    show: {
        opacity: 1,
        transition: { staggerChildren, delayChildren },
    },
});
