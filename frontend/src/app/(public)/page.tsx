import { Hero } from "@/components/home/hero";
import { StatsStrip } from "@/components/home/stats-strip";
import { FeaturesGrid } from "@/components/home/features-grid";

export default function HomePage() {
    return (
        <>
            <Hero />
            <StatsStrip />
            <FeaturesGrid />
        </>
    );
}
