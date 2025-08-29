package com.back.back9.global.problem

import com.back.back9.global.error.ErrorCode
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.net.URI
import java.time.OffsetDateTime

object ProblemDetails {

    fun of(code: ErrorCode, vararg args: Any?): ProblemDetail {
        val detail = if (args.isEmpty()) code.defaultDetail
        else String.format(code.defaultDetail, *args)

        val pd = ProblemDetail.forStatus(code.status)
        pd.title = code.name
        pd.type = URI.create("about:blank")
        pd.setProperty("code", code.code)
        pd.setProperty("timestamp", OffsetDateTime.now())
        // Java 원본과 동일하게 detail은 설정하지 않음(로직 보존)
        return pd
    }

    fun of(
        status: HttpStatus,
        title: String,
        detail: String,
        code: String?,
        properties: Map<String, Any>?
    ): ProblemDetail {
        val pd = ProblemDetail.forStatusAndDetail(status, detail)
        pd.title = title
        if (code != null) pd.setProperty("code", code)
        properties?.forEach { (k, v) -> pd.setProperty(k, v) }
        pd.setProperty("timestamp", OffsetDateTime.now())
        return pd
    }
}
