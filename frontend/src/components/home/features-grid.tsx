import { PageShell } from "@/components/layout/page-shell";

export function FeaturesGrid() {
    const features = [
        { title: "지갑 관리", desc: "보유 코인, 입출금 내역을 한눈에." },
        { title: "거래내역", desc: "코인별 필터/기간 검색/페이지네이션 제공." },
        { title: "관리자 전용", desc: "가상화폐 등록/삭제 및 운영 기능." },
    ];

    return (
        <section className="py-20">
            <PageShell maxW="max-w-[80vw]" padded>
                <h2 className="text-2xl font-bold text-center mb-10">주요 기능</h2>
                <div className="grid gap-6 md:grid-cols-3">
                    {features.map((f) => (
                        <div key={f.title} className="border rounded-lg p-6">
                            <div className="font-semibold text-lg">{f.title}</div>
                            <div className="text-sm text-muted-foreground mt-2">{f.desc}</div>
                        </div>
                    ))}
                </div>
            </PageShell>
        </section>
    );
}