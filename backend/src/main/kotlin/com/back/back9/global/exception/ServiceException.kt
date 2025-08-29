package com.back.back9.global.exception

import com.back.back9.global.rsData.RsData

class ServiceException(
    private val resultCode: String,
    private val msg: String
) : RuntimeException("$resultCode : $msg") {

    val rsData: RsData<Void>
        get() = RsData(resultCode, msg, null)
}
