import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
    output: "standalone",

    env: {
        NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL,
    },

    async rewrites() {
        const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
        console.log('백엔드 API URL:', apiUrl);

        return [
            {
                source: "/api/:path*",
                destination: `${apiUrl}/api/:path*`,
            },
        ];
    },

    // 디버깅용 - 어떤 요청이 들어오는지 확인
    async headers() {
        return [
            {
                source: '/api/:path*',
                headers: [
                    {
                        key: 'x-debug-path',
                        value: 'api-request',
                    },
                ],
            },
        ];
    },
};

export default nextConfig;