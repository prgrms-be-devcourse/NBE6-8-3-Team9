// lib/api/client.ts

// 백엔드 에러 응답 타입 정의
export interface ErrorResponse {
    status: number;
    code: string;
    message: string;
    timestamp?: string;
}

// API 기본 설정 - 환경별 구분 (프론트/NGINX 프록시 전제)
const API_BASE_URL =
    process.env.NEXT_PUBLIC_API_URL ||
    (process.env.NODE_ENV === "production" ? "/api" : "/api");

// 안전한 URL 결합
function joinUrl(base: string, endpoint: string) {
    // endpoint가 /api로 시작하지 않으면 자동으로 /api prefix 추가
    let e = endpoint.startsWith("/api/") ? endpoint : `/api${endpoint.startsWith("/") ? endpoint : `/${endpoint}`}`;
    const b = base.endsWith("/") ? base.slice(0, -1) : base;
    return `${b}${e}`;
}

/**
 * 공통 API 호출 헬퍼 함수
 */
export async function apiCall<T>(
    endpoint: string,
    options: RequestInit = {}
): Promise<T | null> {
    try {
        const response = await fetch(joinUrl(API_BASE_URL, endpoint), {
            // 기본값
            credentials: "include",
            headers: {
                "Content-Type": "application/json",
                ...(options.headers || {}),
            },
            ...options,
        });

        if (response.ok) {
            // 204 등 body 없는 경우
            if (response.status === 204) return null;
            return await response.json();
        }

        // 에러 응답 처리
        let errorData: ErrorResponse;
        try {
            errorData = await response.json();
        } catch {
            // response.json() 파싱 실패 시, text로 한 번 더 시도 (한글 깨짐 디버깅)
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

        const error = new Error(errorData.message) as Error & ErrorResponse;
        error.status = errorData.status;
        error.code = errorData.code;
        throw error;
    } catch (error) {
        if (error instanceof Error && !("status" in error)) {
            console.error("네트워크 에러:", error);
            const networkError = new Error(
                "네트워크 연결을 확인해주세요."
            ) as Error & ErrorResponse;
            networkError.status = 0;
            networkError.code = "NETWORK_ERROR";
            throw networkError;
        }
        throw error;
    }
}
