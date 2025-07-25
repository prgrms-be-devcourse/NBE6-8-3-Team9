import { NextResponse } from "next/server";

export async function POST(req: Request) {
    const body = await req.json();
    // TODO: DB 저장 (server component or prisma 등)
    return NextResponse.json({ ok: true, body });
}
