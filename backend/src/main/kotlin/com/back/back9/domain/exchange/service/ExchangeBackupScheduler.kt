package com.back.back9.domain.exchange.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ExchangeBackupScheduler(private val backupService: ExchangeBackupService) {

    companion object {
        private val log = LoggerFactory.getLogger(ExchangeBackupScheduler::class.java)
    }

    // 매일 자정에 실행
    @Scheduled(cron = "0 0 0 * * *")
    fun scheduleDailyTasks() {
        try {
            // 1. Redis 데이터 백업 실행
            backupService.backupDataFromRedisToDB()
        } catch (e: Exception) {
            log.error("🔥 일일 Redis 데이터 백업 작업 중 심각한 오류가 발생했습니다.", e)
        }

        try {
            // [추가] 2. 백업 작업 후, RDB-Provider 데이터 동기화 실행
            backupService.synchronizeRdbRecordsWithProvider()
        } catch (e: Exception) {
            log.error("🔥 일일 RDB 데이터 동기화 작업 중 심각한 오류가 발생했습니다.", e)
        }
    }
}
