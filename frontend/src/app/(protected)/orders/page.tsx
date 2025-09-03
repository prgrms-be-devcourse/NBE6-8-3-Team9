"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import {format, formatDate} from "date-fns";
import { ko } from "date-fns/locale";
import { CalendarIcon, RotateCcw, AlertCircle } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Checkbox } from "@/components/ui/checkbox"; // shadcn/ui 기준

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { DataTable } from "@/components/ui/data-table";
import {ColumnDef, RowSelectionState} from "@tanstack/react-table";
import { cn } from "@/lib/utils";
import { PageShell } from "@/components/layout/page-shell";
import { ordersApi } from "@/lib/api/orders";
import { apiCall } from "@/lib/api/client";
import type { OrderResponse } from "@/lib/types/orders";
const statusMap: Record<string, string> = {
    PENDING: "대기",
    FILLED: "체결",
    CANCELLED: "취소",
    FAILED: "실패",
    EXPIRED: "만료",
    PARTIALLY_FILLED: "일부체결",
};

const columns: ColumnDef<OrderResponse>[] = [
    {
        id: "select",
        header: ({ table }) => (
            <Checkbox
                checked={table.getIsAllPageRowsSelected()}
                onCheckedChange={(value) => table.toggleAllPageRowsSelected(!!value)}
                aria-label="Select all"
            />
        ),
        cell: ({ row }) => {
            const order = row.original;
            const isPending = order.orderStatus === "PENDING";

            return (
                <Checkbox
                    checked={row.getIsSelected()}
                    onCheckedChange={(value) => row.toggleSelected(!!value)}
                    aria-label="Select row"
                    disabled={!isPending}
                />
            );
        },
        enableSorting: false,
        enableHiding: false,
    },
    {
        accessorKey: "createDate",
        header: () => <div className="text-center">주문일시</div>,
        cell: ({ getValue }) => (
            <div className="text-center">{getValue() as string}</div>
        ),
    },
    {
        accessorKey: "updateDate",
        header: () => <div className="text-center">처리일시</div>,
        cell: ({ getValue }) => (
            <div className="text-center">{getValue() as string}</div>
        ),
    },
    {
        accessorKey: "coinSymbol",
        header: () => <div className="text-center">코인 심볼</div>,
        cell: ({ getValue }) => (
            <div className="text-center">{getValue() as string}</div>
        ),
    },
    {
        accessorKey: "coinName",
        header: () => <div className="text-center">코인 이름</div>,
        cell: ({ getValue }) => (
            <div className="text-center">{getValue() as string}</div>
        ),
    },
    {
        accessorKey: "tradeType",
        header: () => <div className="text-center">구분</div>,
        cell: ({ getValue }) => (
            <div className="text-center">{getValue() as string}</div>
        ),
    },
    {
        accessorKey: "orderMethod",
        header: () => <div className="text-center">주문방식</div>,
        cell: ({ getValue }) => (
            <div className="text-center">{getValue() as string}</div>
        ),
    },
    {
        accessorKey: "price",
        header: () => <div className="text-center">단가</div>,
        cell: ({ getValue }) => (
            <div className="text-center">
                {Number(getValue() as string).toLocaleString()}
            </div>
        ),
    },
    {
        accessorKey: "quantity",
        header: () => <div className="text-center">수량</div>,
        cell: ({ getValue }) => (
            <div className="text-center">{getValue() as string}</div>
        ),
    },
    {
        accessorKey: "orderStatus",
        header: () => <div className="text-center">상태</div>,
        cell: ({ getValue }) => {
            const statusValue = getValue() as string; // 여기서 선언
            return (
                <div className="text-center">
                    {statusMap[statusValue] ?? statusValue}
                </div>
            );
        },
    }
];

const fadeInUp = {
    hidden: { opacity: 0, y: 16 },
    show: { opacity: 1, y: 0, transition: { duration: 0.4 } },
};
const stagger = (delay = 0.1) => ({
    hidden: {},
    show: { transition: { staggerChildren: delay } },
});

interface FilterState {
    startDate: Date | undefined;
    endDate: Date | undefined;
    tradeType: string;    // 매수/매도/전체
    orderType: string;    // LIMIT/MARKET/전체
    orderStatus: string;  // 대기/체결/취소 등
}

export default function TransactionsPage() {
    const router = useRouter();
    const [isLoading, setIsLoading] = useState(true);
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [filters, setFilters] = useState<FilterState>({
        startDate: undefined,
        endDate: undefined,
        tradeType: "전체",
        orderType: "전체",
        orderStatus: "전체",
    });
    const [ordersData, setOrdersData] = useState<OrderResponse[]>([]);
    const [dateError, setDateError] = useState("");
    const [isStartOpen, setIsStartOpen] = useState(false);
    const [isEndOpen, setIsEndOpen] = useState(false);
    const [userId, setUserId] = useState<number | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [rowSelection, setRowSelection] = useState<RowSelectionState>({})

    useEffect(() => {
        const checkAuthAndFetchData = async () => {
            try {
                // API 클라이언트를 사용하여 일관된 URL과 설정으로 인증 확인
                const response = await apiCall<{
                    result: {
                        id: number;
                        userLoginId: string;
                        username: string;
                    };
                    message?: string;
                }>('/api/v1/users/me');
                if (response && (response as any).result?.id) {
                    const currentUserId = (response as any).result.id;
                    setIsAuthenticated(true);
                    setUserId(currentUserId);
                    console.log('현재 사용자 ID:', currentUserId);

                    // userId로 직접 거래 현황 조회 (지갑 조회 과정 생략)
                    await fetchOrdersWithUserId(currentUserId);
                } else {
                    router.replace("/login");
                    return;
                }
            } catch (error) {
                console.error("인증 확인 실패:", error);
                router.replace("/login");
                return;
            } finally {
                setIsLoading(false);
            }
        };

        checkAuthAndFetchData();
    }, [router]);

    // userId로 직접 거래 현황 조회하는 함수
    const fetchOrdersWithUserId = async (userId: number) => {
        try {
            setIsLoading(true);
            console.log('거래 현황 조회 시작 - userId:', userId);
            const response = await ordersApi.getUserOrders(userId);
            console.log("거래 현황 응답:", response);

            if (response !== null) {
                // @ts-ignore
                setOrdersData(response);
            } else {
                console.warn("거래 현황이 없습니다.");
            }
        } catch (error) {
            console.error("거래 현황 조회 실패:", error);
            setError("거래 현황을 불러오는데 실패했습니다: " + (error as any)?.message);
        } finally {
            setIsLoading(false);
        }
    };

    const fetchOrders = async () => {
        if (!userId) return;
        await fetchOrdersWithUserId(userId);
    };

    const handleFilterChange = (key: keyof FilterState, value: any) => {
        const newFilters = { ...filters, [key]: value };
        setFilters(newFilters);
        const startDate = key === "startDate" ? value : newFilters.startDate;
        const endDate = key === "endDate" ? value : newFilters.endDate;

        if (startDate && !endDate) setDateError("종료일을 선택해주세요.");
        else if (!startDate && endDate) setDateError("시작일을 선택해주세요.");
        else if (startDate && endDate && startDate > endDate)
            setDateError("시작일은 종료일보다 이전이어야 합니다.");
        else setDateError("");
    };

    const handleClear = () => {
        setFilters({
            startDate: undefined,
            endDate: undefined,
            tradeType: "전체",
            orderType: "전체",
            orderStatus: "전체",
        });
        setDateError("");
    };

    const handleFilter = async () => {
        if (!userId) return;
        
        const params: Record<string, any> = {};

        if (filters.startDate) {
            params.startDate = formatDate(filters.startDate, 'yyyy-MM-dd');
        }
        if (filters.endDate) {
            params.endDate = formatDate(filters.endDate, 'yyyy-MM-dd');
        }

        // 거래 타입 (매수/매도)
        if (filters.tradeType === "매수") {
            params.tradeType = "BUY";
        } else if (filters.tradeType === "매도") {
            params.tradeType = "SELL";
        }

        // 주문 방식 (LIMIT/MARKET)
        if (filters.orderType !== "전체") {
            params.orderMethod = filters.orderType;
        }
        // 거래 유형이 존재할 경우만 추가
        if (filters.orderStatus === '대기') {
            params.orderStatus = 'PENDING';
        } else if (filters.orderStatus === '체결') {
            params.orderStatus = 'FILLED';
        } else if (filters.orderStatus === '취소') {
            params.orderStatus = 'CANCELLED';
        } else if (filters.orderStatus === '실패') {
            params.orderStatus = 'FAILED';
        } else if (filters.orderStatus === '만료') {
            params.orderStatus = 'EXPIRED';
        } else if (filters.orderStatus === '일부체결') {
            params.orderStatus = 'PARTIALLY_FILLED';
        }
        console.log("필터 적용 - 요청 파라미터:", params);

        try {
            const response = await ordersApi.getFilteredOrders(userId, params);
            // @ts-ignore
            setOrdersData(response);
        } catch (error) {
            console.error("필터 적용 중 오류 발생:", error);
        }
    };

    if (isLoading) {
        return (
            <div className="container py-8 flex items-center justify-center">
                <p>로딩 중...</p>
            </div>
        );
    }

    if (!isAuthenticated) {
        return null;
    }
    const handleCancelOrders = async () => {

        const selectedOrders = ordersData.filter((o) =>
            rowSelection[o.id?.toString()]
        );
        const pendingOrders = selectedOrders.filter(o => o.orderStatus === "PENDING")

        if (pendingOrders.length === 0) {
            alert("대기 상태(PENDING) 주문만 취소할 수 있습니다.")
            return
        }

        try {
            const response = await ordersApi.cancelOrders(pendingOrders.map(o => o.id))
            if (response.isSuccess) {
                alert(`${pendingOrders.length}건의 주문이 성공적으로 취소되었습니다.`)
                setRowSelection({})
                fetchOrders()
            } else {
                alert(`주문 취소 실패: ${response.message || "알 수 없는 오류"}`)
            }
        } catch (err) {
            console.error("취소 실패:", err)
            alert("주문 취소 중 에러가 발생했습니다.")
        }
    }

    // @ts-ignore
    return (
        <PageShell
            maxW="max-w-[80vw]"
            padded
            innerClassName={cn("min-h-[60vh] flex flex-col items-center justify-center text-center space-y-6")}
        >
            <motion.div className="container py-8 space-y-6" variants={stagger(0.1)} initial="hidden" animate="show" suppressHydrationWarning>
                <motion.h1 variants={fadeInUp} className="text-2xl font-bold w-full text-left" suppressHydrationWarning>
                    가상화폐 주문 현황
                </motion.h1>

                {/* 필터 */}
                <motion.div variants={fadeInUp} suppressHydrationWarning>
                    <div className="bg-white p-4 rounded-lg border shadow-sm mb-6">
                        <div className="flex flex-wrap items-center justify-around gap-4">
                            {/* 날짜 선택 */}
                            <div className="flex items-center gap-2">
                                <span className="text-sm font-medium text-gray-700">기간 선택</span>
                                <Popover open={isStartOpen} onOpenChange={setIsStartOpen}>
                                    <PopoverTrigger asChild>
                                        <Button variant="outline" className={`w-40 justify-start ${dateError ? "border-red-500" : ""}`}>
                                            <CalendarIcon className="mr-2 h-4 w-4" />
                                            {filters.startDate ? format(filters.startDate, "yyyy.MM.dd", { locale: ko }) : "시작일 선택"}
                                        </Button>
                                    </PopoverTrigger>
                                    <PopoverContent className="w-auto p-0">
                                        <Calendar
                                            mode="single"
                                            selected={filters.startDate}
                                            onSelect={(date) => {
                                                setIsStartOpen(false);
                                                handleFilterChange("startDate", date);
                                            }}
                                            initialFocus
                                        />
                                    </PopoverContent>
                                </Popover>
                                <span className="text-gray-500">~</span>
                                <Popover open={isEndOpen} onOpenChange={setIsEndOpen}>
                                    <PopoverTrigger asChild>
                                        <Button variant="outline" className={`w-40 justify-start ${dateError ? "border-red-500" : ""}`}>
                                            <CalendarIcon className="mr-2 h-4 w-4" />
                                            {filters.endDate ? format(filters.endDate, "yyyy.MM.dd", { locale: ko }) : "종료일 선택"}
                                        </Button>
                                    </PopoverTrigger>
                                    <PopoverContent className="w-auto p-0">
                                        <Calendar
                                            mode="single"
                                            selected={filters.endDate}
                                            onSelect={(date) => {
                                                setIsEndOpen(false);
                                                handleFilterChange("endDate", date);
                                            }}
                                            initialFocus
                                        />
                                    </PopoverContent>
                                </Popover>
                            </div>
                            
                            {/* 거래타입  */}
                            <div className="flex items-center gap-2">
                                <span className="text-sm font-medium text-gray-700">거래타입 선택</span>
                                <Select
                                    value={filters.tradeType}
                                    onValueChange={(value) => handleFilterChange("tradeType", value)}
                                >
                                    <SelectTrigger className="w-32">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="전체">전체</SelectItem>
                                        <SelectItem value="매수">매수</SelectItem>
                                        <SelectItem value="매도">매도</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                            
                            {/* 주문방식  */}
                            <div className="flex items-center gap-2">
                                <span className="text-sm font-medium text-gray-700">주문방식 선택</span>
                                <Select
                                    value={filters.orderType}
                                    onValueChange={(value) => handleFilterChange("orderType", value)}
                                >
                                    <SelectTrigger className="w-32">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="전체">전체</SelectItem>
                                        <SelectItem value="LIMIT">LIMIT</SelectItem>
                                        <SelectItem value="MARKET">MARKET</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                            
                            {/* 거래 구분 */}
                            <div className="flex items-center gap-2">
                                <span className="text-sm font-medium text-gray-700">거래상태 선택</span>
                                <Select
                                    value={filters.orderStatus}
                                    onValueChange={(value) => handleFilterChange("orderStatus", value)}
                                >
                                    <SelectTrigger className="w-32">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="전체">전체</SelectItem>
                                        <SelectItem value="대기">대기</SelectItem>
                                        <SelectItem value="체결">체결</SelectItem>
                                        <SelectItem value="취소">취소</SelectItem>
                                        <SelectItem value="실패">실패</SelectItem>
                                        <SelectItem value="만료">만료</SelectItem>
                                        <SelectItem value="일부체결">일부체결</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>

                            {/* 버튼 */}
                            <div className="flex items-center gap-2 justify-end">
                                <Button variant="outline" size="sm" onClick={handleClear} className="flex items-center gap-2">
                                    <RotateCcw className="h-4 w-4" />
                                    초기화
                                </Button>
                                <Button
                                    size="sm"
                                    onClick={handleFilter}
                                    className="bg-blue-600 hover:bg-blue-700"
                                    disabled={!!dateError}
                                >
                                    적용
                                </Button>
                                <Button
                                    size="sm"
                                    className="bg-red-600 hover:bg-red-700"
                                    disabled={
                                        Object.keys(rowSelection).length === 0 ||
                                        ordersData.filter((o) => rowSelection[o.id.toString()])
                                            .filter(order => order.orderStatus === "PENDING").length === 0
                                    }
                                    onClick={handleCancelOrders}
                                >
                                    선택 주문 취소
                                </Button>
                            </div>
                        </div>

                        {dateError && (
                            <div className="mt-3 flex items-center gap-2 text-red-600 text-sm">
                                <AlertCircle className="h-4 w-4" />
                                {dateError}
                            </div>
                        )}
                    </div>
                </motion.div>

                {/* 거래 현황 테이블 */}
                <motion.div variants={fadeInUp}>
                    <DataTable
                        columns={columns}
                        data={ordersData}
                        pageSize={10}
                        rowSelection={rowSelection}
                        onRowSelectionChange={(updater) => {
                            const newSelection =
                                typeof updater === "function" ? updater(rowSelection) : updater;
                            console.log("rowSelection change:", newSelection);
                            setRowSelection(newSelection);
                        }}
                    />
                </motion.div>
            </motion.div>
            {/*{userId && <OrderNotification userId={userId} />}*/}

            {/*<Button*/}
            {/*    size="sm"*/}
            {/*    className="bg-green-600 hover:bg-green-700"*/}
            {/*    onClick={async () => {*/}
            {/*        try {*/}
            {/*            const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/orders/mock`, {*/}
            {/*                method: "POST",*/}
            {/*                credentials : "include",*/}
            {/*            });*/}
            {/*            if (res.ok) {*/}
            {/*                alert("거래 현황 15개 생성 완료!");*/}
            {/*                fetchOrders(); // 다시 불러오기*/}
            {/*            } else {*/}
            {/*                alert("생성 실패");*/}
            {/*            }*/}
            {/*        } catch (err) {*/}
            {/*            console.error("Mock 생성 실패:", err);*/}
            {/*            alert("에러 발생");*/}
            {/*        }*/}
            {/*    }}*/}
            {/*>*/}
            {/*    거래현황 생성*/}
            {/*</Button>*/}

        </PageShell>
    );
}