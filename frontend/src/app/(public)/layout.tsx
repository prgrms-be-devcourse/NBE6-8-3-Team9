import { MainNav } from "@/components/layout/main-nav";
import { Footer } from "@/components/layout/footer";
import { PageShell } from "@/components/layout/page-shell";

export default function PublicLayout({ children }: { children: React.ReactNode }) {
    return (
        <div className="min-h-screen flex flex-col">
            <header className="w-full">
                <PageShell maxW="max-w-[80vw]" padded>
                    <MainNav />
                </PageShell>
            </header>

            <main className="flex-1 w-full">
                {/* 본문도 동일한 max-width로 가운데 정렬 */}
                <PageShell maxW="max-w-[80vw]" padded>
                    {children}
                </PageShell>
            </main>

            <footer className="w-full">
                <PageShell maxW="max-w-[80vw]" padded>
                    <Footer />
                </PageShell>
            </footer>
        </div>
    );
}
