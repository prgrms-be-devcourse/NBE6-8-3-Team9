import { NextResponse } from "next/server";

export async function POST(req: Request) {
    const { email, password } = await req.json();

    // TODO: 실제 인증 로직 (DB 조회 등)
    const ok = email === "admin@test.com" && password === "password";

    if (!ok) {
        return NextResponse.json({ message: "이메일 또는 비밀번호가 올바르지 않습니다." }, { status: 401 });
    }

    const res = NextResponse.json({
        ok: true,
        user: { email, role: "ADMIN" },
    });

    // 실제로는 JWT 등을 넣으세요.
    res.cookies.set("access_token", "FAKE_TOKEN", {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        path: "/",
        maxAge: 60 * 60 * 24,
    });

    res.cookies.set("role", "ADMIN", {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        path: "/",
        maxAge: 60 * 60 * 24,
    });

    return res;
}
