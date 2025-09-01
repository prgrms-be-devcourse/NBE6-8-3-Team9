package com.back.back9.domain.orders.trigger.service

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * 앱 부팅 시 DB의 PENDING 예약들을 Redis로 재인덱싱.
 * - Redis가 초기화되었거나 앱이 비정상 종료되었어도 예약이 살아나도록 함.
 */
@Component
class TriggerStartupRebuilder(
    private val engine: TriggerService
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        engine.rebuildAllPending()
    }
}