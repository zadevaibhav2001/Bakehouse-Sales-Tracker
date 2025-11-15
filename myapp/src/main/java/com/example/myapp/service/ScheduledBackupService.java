package com.example.myapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledBackupService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledBackupService.class);

    @Autowired
    private BackupService backupService;

    @Scheduled(cron = "0 0 * * * ?") // Every hour at the top of the hour
    public void performScheduledBackup() {
        try {
            logger.info("Starting scheduled database backup...");
            String backupLocation = backupService.backupDatabase();
            logger.info("Scheduled backup completed successfully: {}", backupLocation);
        } catch (Exception e) {
            logger.error("Scheduled backup failed: {}", e.getMessage(), e);
        }
    }
}