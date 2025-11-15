package com.example.myapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.attribute.FileTime;

@Service
public class BackupService {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${app.database.file-path:/opt/bakehouse/myapp/data/myapp.mv.db}")
    private String dbFilePath;

    private final S3Client s3Client;
    private final String lastBackupTimeFile = "/tmp/last-backup-time.txt";

    public BackupService() {
        this.s3Client = S3Client.builder()
                .region(Region.AP_SOUTH_1)
                .build();
    }

    public String backupDatabase() throws IOException {
        Path dbPath = Paths.get(dbFilePath);
        if (!Files.exists(dbPath)) {
            throw new IOException("Database file not found: " + dbFilePath);
        }

        // Check if database has changed since last backup
        FileTime currentModTime = Files.getLastModifiedTime(dbPath);
        FileTime lastBackupTime = getLastBackupTime();
        if (lastBackupTime != null && !currentModTime.toInstant().isAfter(lastBackupTime.toInstant())) {
            return "No changes detected - backup skipped";
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String backupFileName = "bakehouse-backup-" + timestamp + ".db.gz";

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

        // Clean up old backups, keep only 10 most recent
        cleanupOldBackups();

        // Update last backup time
        saveLastBackupTime(currentModTime);

        return "s3://" + bucketName + "/backups/" + backupFileName;
    }

    private void cleanupOldBackups() {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix("backups/bakehouse-backup-")
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            List<S3Object> backups = listResponse.contents().stream()
                    .sorted((a, b) -> b.lastModified().compareTo(a.lastModified()))
                    .collect(Collectors.toList());

            if (backups.size() > 10) {
                List<S3Object> toDelete = backups.subList(10, backups.size());
                for (S3Object backup : toDelete) {
                    DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(backup.key())
                            .build();
                    s3Client.deleteObject(deleteRequest);
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the backup
            System.err.println("Failed to cleanup old backups: " + e.getMessage());
        }
    }

    private FileTime getLastBackupTime() {
        try {
            Path file = Paths.get(lastBackupTimeFile);
            if (Files.exists(file)) {
                String timeStr = Files.readString(file).trim();
                return FileTime.fromMillis(Long.parseLong(timeStr));
            }
        } catch (Exception e) {
            // Ignore errors, treat as no previous backup
        }
        return null;
    }

    private void saveLastBackupTime(FileTime time) {
        try {
            Files.writeString(Paths.get(lastBackupTimeFile), String.valueOf(time.toMillis()));
        } catch (Exception e) {
            System.err.println("Failed to save last backup time: " + e.getMessage());
        }
    }
}