package com.back.back9.global.error

class ErrorException(
    val errorCode: ErrorCode,
    vararg val args: Any?
) : RuntimeException(errorCode.name)
