// lib/api/client.ts

export interface ErrorResponse {
    status: number;
    code: string;
    message: string;
    timestamp?: string;
}

export async function apiCall<T>(
    endpoint: string,
    options: RequestInit = {}
): Promise<T | null> {
    let response: Response;

    try {
        response = await fetch(endpoint, {
            credentials: "include",
            headers: {
                "Content-Type": "application/json",
                ...(options.headers || {}),
            },
            ...options,
        });
    } catch (err) {
        console.error("네트워크 에러:", err);
        throw {
            status: 0,
            code: "NETWORK_ERROR",
            message: "네트워크 연결을 확인해주세요.",
        } as ErrorResponse;
    }

    if (response.ok) {
        if (response.status === 204) return null;
        return await response.json();
    }

    // HTTP 에러 처리
    let errorData: ErrorResponse;

    try {
        errorData = await response.json();
    } catch {
        try {
            const text = await response.text();
            errorData = {
                status: response.status,
                code: `${response.status}-ERROR`,
                message: text || response.statusText || "서버 오류가 발생했습니다.",
            };
        } catch {
            errorData = {
                status: response.status,
                code: `${response.status}-ERROR`,
                message: response.statusText || "서버 오류가 발생했습니다.",
            };
        }
    }

    throw errorData; // ErrorResponse 객체 던짐
}
