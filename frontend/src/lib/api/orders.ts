import { OrdersRequest, OrderResponse } from "@/lib/types/orders";
import {apiCall} from "@/lib/api/client";
import type {TradeLogResponse} from "@/lib/types/tradelog";

export const ordersApi = {
    // 공통 실행 함수
    executeTrade: async (
        walletId: number,
        payload: OrdersRequest
    ): Promise<OrderResponse> => {
        const res = await fetch(`/api/orders/wallet/${walletId}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });

        if (!res.ok) {
            const error = await res.text();
            throw new Error(`Failed to execute trade: ${error}`);
        }

        return res.json();
    },

    // 매수용 함수
    executeTradeByBuying: async (
        walletId: number,
        payload: Omit<OrdersRequest, "tradeType">
    ): Promise<OrderResponse> => {
        return ordersApi.executeTrade(walletId, {
            ...payload,
            tradeType: "BUY",
        });
    },

    // 매도용 함수
    executeTradeBySelling: async (
        walletId: number,
        payload: Omit<OrdersRequest, "tradeType">
    ): Promise<OrderResponse> => {
        return ordersApi.executeTrade(walletId, {
            ...payload,
            tradeType: "SELL",
        });
    },

    // 사용자의 거래 현황 조회 - 새로운 userId 기반 API 사용
    getUserOrders: (userId: number) =>
        apiCall<OrderResponse[]>(`/api/orders/wallet/${userId}`),

    getFilteredOrders: async (
        userId: number,
        filters: Record<string, any>
    ): Promise<OrderResponse[]> => {
        const query = new URLSearchParams();

        Object.entries(filters).forEach(([key, value]) => {
            if (value !== undefined && value !== null) {
                query.append(key, String(value));
            }
        });

        const result = await apiCall<OrderResponse[]>(
            `/api/orders/wallet/${userId}?${query.toString()}`
        );

        return result ?? []; // null이면 빈 배열 반환
    },

    cancelOrders: async (orderIds: number[]): Promise<{ isSuccess: boolean; message?: string }> => {
        const res = await fetch(`/api/orders/cancel`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({ orderIds }),
        });

        if (!res.ok) {
            const error = await res.text();
            throw new Error(`Failed to cancel orders: ${error}`);
        }

        return res.json();
    },

};