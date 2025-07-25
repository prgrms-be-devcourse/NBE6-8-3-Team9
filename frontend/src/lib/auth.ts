import { cookies } from "next/headers";

export type Role = "ADMIN" | "USER";

export async function getSessionFromCookie() {
    const cookieStore = await cookies(); // Next 15/edge에서 Promise임
    const token = cookieStore.get("access_token")?.value;
    const role = (cookieStore.get("role")?.value as Role) ?? "USER";

    return {
        isAuthenticated: !!token,
        role,
        user: token ? { email: "you@example.com", role } : null,
    };
}
