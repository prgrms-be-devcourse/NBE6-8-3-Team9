package com.back.back9.global.rsData

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class RsData<T>(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("code")
    val code: Int,
    @JsonProperty("message")
    val message: String,
    @JsonProperty("result")
    val result: T?,
    @JsonIgnore
    val resultCode: String,
    @JsonIgnore
    val statusCode: Int, // 접근 제한자 제거 (public)
    @JsonIgnore
    val msg: String,
    @JsonIgnore
    val data: T?
) {
    constructor(resultCode: String, msg: String) : this(
        status = if (resultCode.startsWith("2")) "success" else "fail",
        code = resultCode.split("-", limit = 2)[0].toInt(),
        message = msg,
        result = null,
        resultCode = resultCode,
        statusCode = resultCode.split("-", limit = 2)[0].toInt(),
        msg = msg,
        data = null
    )

    constructor(resultCode: String, msg: String, data: T?) : this(
        status = if (resultCode.startsWith("2")) "success" else "fail",
        code = resultCode.split("-", limit = 2)[0].toInt(),
        message = msg,
        result = data,
        resultCode = resultCode,
        statusCode = resultCode.split("-", limit = 2)[0].toInt(),
        msg = msg,
        data = data
    )
}