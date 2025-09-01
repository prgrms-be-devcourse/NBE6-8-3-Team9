package com.back.back9.domain.orders.trigger.support

object RedisKeys {
    fun latestPrice(symbol: String) = "price:$symbol:latest"            // 최신가(선택)
    fun upZset(symbol: String) = "zset:triggers:price:$symbol:up"       // score=threshold
    fun downZset(symbol: String) = "zset:triggers:price:$symbol:down"   // score=threshold
    fun firedFlag(id: String) = "fired:trigger:$id"                     // 중복 실행 방지용 (SETNX)
    fun triggerHash(id: String) = "trigger:$id"                         // 발화 시 필요한 메타
}