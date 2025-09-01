// 유저 관련 dto

export interface UserDto {
    id: number;
    username: string;
    userLoginId: string;
    role: 'ADMIN' | 'MEMBER';
    createdAt: string;
    modifiedAt: string;
}

export interface userWithUsernameDto {
    id: number;
    username: string;
    userLoginId: string;
    role: 'ADMIN' | 'MEMBER';
    createdAt: string;
    modifiedAt: string;
}

export interface userRegisterDto {
    userLoginId: string;
    username: string;
    password: string;
    confirmPassword: string;
}

export interface userLoginReqBody {
    userLoginId: string;
    password: string;
}

export interface userLoginResBody {
    item: UserDto;
    apiKey: string;
    accessToken: string;
}

// 공통 API 응답 타입 (백엔드 RsData 구조와 일치)
export interface ApiResponse<T = any> {
    status: string;   // "success" | "fail"
    code: number;     // 200, 400 등
    message: string;  // 에러/성공 메시지
    result: T | null;
}
