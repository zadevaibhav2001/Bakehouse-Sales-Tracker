package com.example.myapp.controller;

import com.example.myapp.service.BackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    @Autowired
    private BackupService backupService;

    @PostMapping("/database")
    public ResponseEntity<?> backupDatabase() {
        try {
            String backupLocation = backupService.backupDatabase();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Database backup completed successfully",
                "location", backupLocation
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Backup failed: " + e.getMessage()
            ));
        }
    }
}