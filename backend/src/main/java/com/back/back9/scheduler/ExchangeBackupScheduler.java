package com.back.back9.scheduler;

import com.back.back9.domain.exchange.service.ExchangeBackupService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExchangeBackupScheduler {

    private final ExchangeBackupService backupService;

    public ExchangeBackupScheduler(ExchangeBackupService backupService) {
        this.backupService = backupService;
    }

    @Scheduled(cron = "0 0 0 * * *") // 매일 00:00
    public void scheduleDailyBackup() {
        backupService.backupPreviousDayRedisToDB();
    }
}