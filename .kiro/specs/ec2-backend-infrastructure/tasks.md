# Implementation Plan

- [ ] 1. Set up AWS infrastructure resources
  - Create Security Group with inbound rules for SSH (your IP), HTTP (0.0.0.0/0), and HTTPS (0.0.0.0/0)
  - Create S3 bucket for database backups with encryption enabled
  - Create IAM role for EC2 with S3 and CloudWatch permissions
  - Allocate Elastic IP for stable public addressing
  - _Requirements: 1.1, 1.4, 7.1_

- [ ] 2. Prepare EC2 user-data bootstrap script
  - Create user-data script with all configuration variables (DB_PASSWORD, S3_BUCKET, REGION)
  - Include system updates and package installations (Java 17, PostgreSQL, Nginx, AWS CLI, CloudWatch agent)
  - Add PostgreSQL initialization and database/user creation commands
  - Add systemd service configuration for Spring Boot application
  - Add Nginx configuration for reverse proxy and static website serving
  - Add backup script creation and cron job setup
  - _Requirements: 1.1, 1.2, 1.5, 4.1, 5.1_

- [ ] 3. Launch and configure EC2 instance
  - Launch t4g.micro or t3.micro instance in ap-south-1 with Ubuntu 22.04 LTS AMI
  - Attach IAM role and Security Group
  - Associate Elastic IP with instance
  - Verify all services started correctly (PostgreSQL, Nginx)
  - Test SSH access and verify bootstrap script execution
  - _Requirements: 1.1, 1.4, 7.1_

- [ ] 4. Create Spring Boot project structure
  - Initialize Maven project with Spring Boot 3.2.0 parent
  - Add dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, postgresql driver
  - Create package structure: controller, service, model, repository
  - Create Application.java main class with @SpringBootApplication
  - _Requirements: 2.1, 8.1_

- [ ] 5. Implement database entity and repository
  - Create Entry entity with id, userId, payload (JSONB), updatedAt, deleted fields
  - Add JPA annotations and index on (userId, updatedAt)
  - Create EntryRepository interface extending JpaRepository
  - Add custom query method findByUserIdAndUpdatedAtAfter
  - _Requirements: 2.3, 8.1, 8.4_

- [ ] 6. Create sync API DTOs
  - Create PushRequest DTO with userId and list of EntryDto
  - Create EntryDto with id, payload, updatedAt, deleted fields
  - Create PushResponse DTO with accepted count and conflicts list
  - Create PullResponse DTO with entries list and serverTime
  - Create ConflictRecord DTO for conflict reporting
  - _Requirements: 2.1, 2.2, 2.3_

- [ ] 7. Implement SyncService with conflict resolution
  - Create SyncService class with handlePush method
  - Implement last-write-wins conflict resolution using updatedAt timestamps
  - Handle INSERT operations for new entries
  - Handle UPDATE operations with timestamp comparison
  - Create handlePull method to fetch entries modified after given timestamp
  - Return appropriate conflict records when server version is newer
  - _Requirements: 2.2, 2.3, 3.4_

- [ ] 8. Implement REST API controllers
  - Create SyncController with @RestController and @RequestMapping("/api")
  - Implement POST /api/sync/push endpoint calling SyncService.handlePush
  - Implement GET /api/sync/pull endpoint with userId and optional since parameters
  - Implement GET /api/health endpoint returning "OK"
  - Add proper HTTP status codes and error handling
  - _Requirements: 2.1, 2.2, 2.4_

- [ ] 9. Configure application properties
  - Create application.yml with server port 8080 and address 127.0.0.1
  - Configure PostgreSQL datasource with jdbc URL, username, password from environment
  - Configure HikariCP connection pool settings (max 10, min 2)
  - Configure JPA/Hibernate with validate ddl-auto and PostgreSQL dialect
  - Add Spring Boot Actuator health endpoint configuration
  - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [ ] 10. Create database schema initialization
  - Create SQL script for entries table with all required columns
  - Add index on (user_id, updated_at) for query optimization
  - Document manual execution steps for initial schema setup
  - _Requirements: 2.3, 8.1_

- [ ] 11. Build and package application
  - Configure Maven build plugin for Spring Boot fat JAR
  - Run mvn clean package to create executable JAR
  - Verify JAR contains all dependencies and can run standalone
  - _Requirements: 1.2, 2.1_

- [ ] 12. Create deployment script
  - Create deploy.sh script with SCP command to upload JAR to EC2
  - Add SSH command to restart myapp.service
  - Add health check verification after deployment
  - Make script executable and test deployment flow
  - _Requirements: 1.2, 1.5_

- [ ] 13. Deploy application to EC2
  - Upload application JAR to /home/appuser/app/myapp.jar
  - Upload application.yml with correct database credentials
  - Start myapp.service using systemctl
  - Verify application starts and connects to PostgreSQL
  - Check logs using journalctl -u myapp
  - _Requirements: 1.2, 1.5, 8.1, 8.2_

- [ ] 14. Configure Nginx reverse proxy
  - Verify Nginx configuration routes /api/* to localhost:8080
  - Test API endpoint accessibility through Nginx
  - Deploy sample static website files to /var/www/mywebsite
  - Verify static content serving at root path
  - Test both HTTP and HTTPS access
  - _Requirements: 1.3, 2.1, 2.5, 6.1, 6.2, 6.3_

- [ ] 15. Set up SSL/TLS with Let's Encrypt
  - Run certbot --nginx with domain name
  - Verify SSL certificate installation
  - Test HTTPS access to both API and static website
  - Verify auto-renewal cron job is configured
  - _Requirements: 6.4_

- [ ] 16. Configure and start CloudWatch agent
  - Create CloudWatch agent configuration JSON with CPU, memory, disk metrics
  - Add log collection for Spring Boot and Nginx logs
  - Start CloudWatch agent service
  - Verify metrics appear in CloudWatch console
  - Create CloudWatch dashboard for key metrics
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 17. Test backup script and S3 integration
  - Manually run /usr/local/bin/pg_backup_to_s3.sh
  - Verify backup file appears in S3 bucket
  - Check backup log at /var/log/pg_backup.log
  - Verify cron job is scheduled for daily execution at 2:30 AM
  - Test restore procedure from S3 backup
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ] 18. Configure S3 lifecycle policy
  - Create S3 lifecycle rule to transition backups to Glacier after 30 days
  - Add rule to delete backups after 90 days
  - Verify policy is active on backup bucket
  - _Requirements: 4.3, 7.3_

- [ ] 19. Set up CloudWatch alarms
  - Create alarm for CPU utilization > 80%
  - Create alarm for disk usage > 80%
  - Create alarm for memory usage > 80%
  - Configure SNS topic for alarm notifications (optional)
  - Test alarm triggers
  - _Requirements: 5.1, 5.2, 5.3_

- [ ] 20. Perform end-to-end API testing
  - Test POST /api/sync/push with sample data
  - Verify data persists in PostgreSQL database
  - Test GET /api/sync/pull with userId and since parameters
  - Test conflict resolution with competing timestamps
  - Test health endpoint accessibility
  - Verify proper HTTP status codes and error responses
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.4_

- [ ] 21. Create integration tests
  - Set up Testcontainers for PostgreSQL integration testing
  - Write tests for SyncService conflict resolution logic
  - Write tests for EntryRepository custom queries
  - Write MockMvc tests for API endpoints
  - _Requirements: 2.2, 2.3, 3.4_

- [ ] 22. Document mobile client integration
  - Create documentation for sync API contract (push/pull endpoints)
  - Document conflict resolution strategy (last-write-wins)
  - Provide example JSON requests and responses
  - Document recommended local storage approach (SQLite with synced flag)
  - Include sync flow diagram and algorithm steps
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 23. Create operational runbook
  - Document troubleshooting commands (check logs, database, services)
  - Document deployment procedure and rollback steps
  - Document backup and restore procedures
  - Document SSL certificate renewal process
  - Document scaling and migration path
  - _Requirements: 1.5, 4.1, 4.4_

- [ ] 24. Perform load testing
  - Set up JMeter or Gatling for load testing
  - Test with 100 concurrent users making sync requests
  - Measure response times and throughput
  - Identify performance bottlenecks
  - Document capacity limits and optimization recommendations
  - _Requirements: 2.2, 8.4_

- [ ] 25. Verify Free Tier compliance
  - Check EC2 instance hours usage (should be under 750 hours/month)
  - Monitor EBS storage usage (should be under 30 GB)
  - Monitor S3 storage usage (should be under 5 GB)
  - Monitor data transfer usage (should be under 15 GB outbound/month)
  - Set up AWS Budget alert at $10/month threshold
  - _Requirements: 7.1, 7.2, 7.3, 7.4_
