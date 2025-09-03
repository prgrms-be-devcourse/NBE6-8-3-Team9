// app/api/auth/google/callback/route.ts
import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
    const isProd = process.env.NODE_ENV === "production";
    const BASE_URL = isProd
        ? "https://d64t5u28gt0rl.cloudfront.net"
        : `http://localhost:${process.env.PORT || 3000}`;

    const error = req.nextUrl.searchParams.get("error");
    if (error) return NextResponse.redirect(`${BASE_URL}/login?error=oauth_error`);

    // 백엔드가 발행한 HttpOnly 쿠키가 있나만 체크
    const hasLogin = Boolean(req.cookies.get("accessToken") || req.cookies.get("JSESSIONID"));
    if (!hasLogin) return NextResponse.redirect(`${BASE_URL}/login?error=no_session`);

    return NextResponse.redirect(`${BASE_URL}/exchange`);
}