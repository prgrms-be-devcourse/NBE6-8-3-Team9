"use client";

import * as React from "react";
import {
    ColumnDef,
    flexRender,
    getCoreRowModel,
    useReactTable,
    getPaginationRowModel, RowSelectionState, OnChangeFn, Row,
} from "@tanstack/react-table";
import {
    Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import {
    Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";

type DataTableProps<TData, TValue> = {
    columns: ColumnDef<TData, TValue>[];
    data: TData[];
    pageSize?: number;
    rowSelection?: RowSelectionState
    // @ts-ignore
    onRowSelectionChange?: OnChangeFn<RowSelectionState>
    getRowId?: (row: TData, index: number, parent?: Row<TData>) => string; // ✅ 선택적으로 받기

};

export function DataTable<TData, TValue>({
                                             columns,
                                             data,
                                             pageSize = 10,
                                             rowSelection,
                                             onRowSelectionChange,
                                             getRowId, // ✅ 부모에서 받음
                                         }: DataTableProps<TData, TValue>) {
    const table = useReactTable({
        data,
        columns,
        initialState: {
            pagination: { pageIndex: 0, pageSize },
        },
        state: {
            rowSelection,
        },
        onRowSelectionChange,
        getCoreRowModel: getCoreRowModel(),
        getPaginationRowModel: getPaginationRowModel(),
        getRowId: getRowId ?? ((row: any, index: number) => {
            if (row.id !== undefined && row.id !== null) {
                return String(row.id);       // ✅ DB id 우선
            }
            return String(index);          // ✅ fallback
        }),    });

    return (
        <div className="space-y-4">
            <div className="rounded-md border">
                <Table>
                    <TableHeader className="bg-muted/50">
                        {table.getHeaderGroups().map((hg) => (
                            <TableRow key={hg.id}>
                                {hg.headers.map((h) => (
                                    <TableHead key={h.id} className="text-center">
                                        {h.isPlaceholder
                                            ? null
                                            : flexRender(h.column.columnDef.header, h.getContext())}
                                    </TableHead>
                                ))}
                            </TableRow>
                        ))}
                    </TableHeader>
                    <TableBody>
                        {table.getRowModel().rows.length ? (
                            table.getRowModel().rows.map((row) => (
                                <TableRow key={row.id} data-state={row.getIsSelected() && "selected"}>
                                    {row.getVisibleCells().map((cell) => (
                                        <TableCell key={cell.id}>
                                            {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                        </TableCell>
                                    ))}
                                </TableRow>
                            ))
                        ) : (
                            <TableRow>
                                <TableCell colSpan={columns.length} className="h-24 text-center">
                                    No results.
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </div>

            {/* 페이지네이션 */}
            <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                    <span className="text-sm">Rows per page</span>
                    <Select
                        value={String(table.getState().pagination.pageSize)}
                        onValueChange={(v) => table.setPageSize(Number(v))}
                    >
                        <SelectTrigger className="w-[80px]">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {[10, 20, 30, 40, 50].map((s) => (
                                <SelectItem key={s} value={String(s)}>
                                    {s}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>

                <div className="flex items-center space-x-2">
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => table.previousPage()}
                        disabled={!table.getCanPreviousPage()}
                    >
                        이전
                    </Button>
                    <span className="text-sm">
            {table.getState().pagination.pageIndex + 1} / {table.getPageCount()}
          </span>
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => table.nextPage()}
                        disabled={!table.getCanNextPage()}
                    >
                        다음
                    </Button>
                </div>
            </div>
        </div>
    );
}
