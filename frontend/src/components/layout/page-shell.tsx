// src/components/layout/page-shell.tsx
"use client";

import React, {JSX} from "react";
import { cn } from "@/lib/utils";

type PageShellProps = {
    children: React.ReactNode;

    /** 가로 가운데 정렬 + maxW 적용 (기본 true) */
    center?: boolean;

    /** 기본 "max-w-5xl" */
    maxW?: string;

    /** 좌우 패딩(px-4 md:px-6 lg:px-8) (기본 true) */
    padded?: boolean;

    /** 세로까지 가운데 정렬 (기본 false) */
    vCenter?: boolean;

    /**
     * 뷰포트 높이를 채울지 여부 (기본 false)
     * - true면 min-h-screen 혹은 calc(100vh - header - footer) 사용
     */
    fullHeight?: boolean;

    /**
     * 헤더/푸터 높이를 제외한 높이를 쓰고 싶을 때 (기본 false)
     * - globals.css 등에 --header-h, --footer-h CSS 변수를 선언해두면 계산값을 사용
     */
    withChromeOffset?: boolean;

    /** 특정 페이지에서 내부 div에만 className을 주고 싶을 때 */
    innerClassName?: string;

    /** 필요하면 추가 클래스 */
    className?: string;

    /** div 대신 다른 태그로 감싸고 싶을 때 */
    as?: keyof JSX.IntrinsicElements;
};

export function PageShell({
                              children,
                              center = true,
                              maxW = "max-w-5xl",
                              padded = true,
                              vCenter = false,
                              fullHeight = false,
                              withChromeOffset = false,
                              innerClassName,
                              className,
                              as = "div",
                          }: PageShellProps) {
    const Wrapper = as as any;

    const minHClass = fullHeight
        ? withChromeOffset
            ? "min-h-[calc(100vh-var(--header-h,64px)-var(--footer-h,260px))]"
            : "min-h-screen"
        : undefined;

    const outerClass = cn(
        minHClass,
        vCenter ? "grid place-items-center" : undefined,
        className
    );

    const innerClass = cn(
        "w-full",
        center ? `${maxW} mx-auto` : "max-w-none",
        padded && "px-4 md:px-6 lg:px-8",
        innerClassName
    );

    return (
        <Wrapper className={outerClass}>
            <div className={innerClass}>{children}</div>
        </Wrapper>
    );
}
