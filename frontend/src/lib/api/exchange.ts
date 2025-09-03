import {ExchangeDTO, InitialRequestDTO, PreviousRequestDTO} from "@/lib/types/exchange/type";


/**
 * 비동기 함수의 중복 호출을 방지하는 고차 함수
 */
function preventDuplicateCalls<T extends (...args: any[]) => Promise<any>>(asyncFn: T): T {
    const inFlightRequests = new Map<string, Promise<ReturnType<T>>>();
    return async function(...args: Parameters<T>): Promise<ReturnType<T>> {
        const key = JSON.stringify(args);
        if (inFlightRequests.has(key)) {
            console.log("🚀 동일한 요청 진행 중, 기존 요청 반환:", key);
            return inFlightRequests.get(key)!;
        }
        console.log("✅ 신규 API 요청 시작:", key);
        const promise = asyncFn(...args);
        inFlightRequests.set(key, promise);
        promise.finally(() => {
            inFlightRequests.delete(key);
        });
        return promise;
    } as T;
}

const exchange = {
    getLatest: async (): Promise<ExchangeDTO[]> => {
        const res = await fetch("/api/exchange/coins-latest");
        if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
        return res.json();
    },
    getInitialCandles: async (payload: InitialRequestDTO): Promise<ExchangeDTO[]> => {
        const res = await fetch("/api/exchange/initial", {
            method: "POST",
            body: JSON.stringify(payload),
            headers: { "Content-Type": "application/json" },
        });
        if (!res.ok) throw new Error("초기 캔들 데이터 로딩 실패");
        return res.json();
    },
    getPreviousCandles: async (payload: PreviousRequestDTO): Promise<ExchangeDTO[]> => {
        const res = await fetch("/api/exchange/previous", {
            method: "POST",
            body: JSON.stringify(payload),
            headers: { "Content-Type": "application/json" },
        });
        if (!res.ok) throw new Error("과거 캔들 데이터 로딩 실패");
        return res.json();
    },
};

export const exchangeApi = {
    getLatest: preventDuplicateCalls(exchange.getLatest),
    getInitialCandles: preventDuplicateCalls(exchange.getInitialCandles),
    getPreviousCandles: preventDuplicateCalls(exchange.getPreviousCandles),
};
