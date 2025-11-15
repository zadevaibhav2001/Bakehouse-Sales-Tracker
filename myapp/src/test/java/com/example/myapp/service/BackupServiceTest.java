package com.example.myapp.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "aws.s3.bucket-name=test-bucket",
    "app.database.file-path=./data/myapp.mv.db"
})
class BackupServiceTest {

    @Test
    void testBackupServiceCreation() {
        BackupService backupService = new BackupService();
        assertNotNull(backupService);
    }
}