package com.example.myapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;

@Service
public class BackupService {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${app.database.file-path:/opt/bakehouse/myapp/data/myapp.mv.db}")
    private String dbFilePath;

    private final S3Client s3Client;

    public BackupService() {
        this.s3Client = S3Client.builder()
                .region(Region.AP_SOUTH_1)
                .build();
    }

    public String backupDatabase() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String backupFileName = "bakehouse-backup-" + timestamp + ".db.gz";
        
        Path dbPath = Paths.get(dbFilePath);
        if (!Files.exists(dbPath)) {
            throw new IOException("Database file not found: " + dbFilePath);
        }

        // Create compressed backup
        Path tempBackupPath = Paths.get("/tmp/" + backupFileName);
        try (FileInputStream fis = new FileInputStream(dbPath.toFile());
             FileOutputStream fos = new FileOutputStream(tempBackupPath.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, len);
            }
        }

        // Upload to S3
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key("backups/" + backupFileName)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromFile(tempBackupPath));

        // Clean up temp file
        Files.deleteIfExists(tempBackupPath);

        return "s3://" + bucketName + "/backups/" + backupFileName;
    }
}