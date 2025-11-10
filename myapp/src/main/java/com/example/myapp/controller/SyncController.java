package com.example.myapp.controller;

import com.example.myapp.dto.PullResponse;
import com.example.myapp.dto.PushRequest;
import com.example.myapp.dto.PushResponse;
import com.example.myapp.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class SyncController {

    private final SyncService syncService;

    /**
     * Push endpoint - Client pushes new/updated entries to server
     * POST /api/sync/push
     */
    @PostMapping("/sync/push")
    public ResponseEntity<PushResponse> push(@RequestBody PushRequest request) {
        log.info("Received push request from user: {}", request.getUserId());
        
        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        if (request.getEntries() == null) {
            return ResponseEntity.badRequest().build();
        }

        PushResponse response = syncService.handlePush(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Pull endpoint - Client fetches updated entries from server
     * GET /api/sync/pull?userId=xxx&since=2025-11-10T12:00:00Z
     */
    @GetMapping("/sync/pull")
    public ResponseEntity<PullResponse> pull(
            @RequestParam String userId,
            @RequestParam(required = false) String since) {
        
        log.info("Received pull request from user: {} since: {}", userId, since);
        
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        PullResponse response = syncService.handlePull(userId, since);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
