"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { authApi } from "@/lib/api/auth"; // authApi import로 변경

// 쿠키 삭제 유틸 함수
function deleteCookie(name: string) {
    const isProduction = process.env.NODE_ENV === 'production';
    
    if (isProduction) {
        // 배포 환경 - .onrender.com 도메인용
        document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=.onrender.com; secure; SameSite=None`;
    }
    // 로컬 환경 및 fallback
    document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
}

export default function LogoutPage() {
    const router = useRouter();

    useEffect(() => {
        const logout = async () => {
            try {
                // authApi.logout 사용 (일관성 있는 API 호출)
                await authApi.logout();
                console.log('백엔드 로그아웃 성공');
            } catch (error) {
                console.warn('백엔드 로그아웃 실패:', error);
            } finally {
                // 프론트엔드에서도 쿠키 명시적 삭제 (배포 환경 대응)
                deleteCookie("accessToken");
                deleteCookie("apiKey");
                deleteCookie("role");
                
                // 백엔드에서 쿠키 삭제를 처리하므로 프론트엔드에서는 바로 리다이렉트
                setTimeout(() => {
                    router.replace("/login");
                }, 500);
            }
        };

        logout();
    }, [router]);

    return (
        <div className="container py-8 flex items-center justify-center">
            <div className="text-center">
                <p>로그아웃 중...</p>
                <p className="text-sm text-gray-500 mt-2">잠시만 기다려주세요.</p>
            </div>
        </div>
    );
}
