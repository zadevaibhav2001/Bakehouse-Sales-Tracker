package com.example.myapp.controller;

import com.example.myapp.dto.*;
import com.example.myapp.service.SyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SyncController.class)
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SyncService syncService;

    @Test
    void testPush_ValidRequest_ShouldReturnOk() throws Exception {
        // Given
        EntryDto entryDto = new EntryDto("id1", "{\"data\":\"test\"}", Instant.now(), false);
        PushRequest request = new PushRequest("user123", Arrays.asList(entryDto));
        PushResponse response = new PushResponse(1, Collections.emptyList());
        
        when(syncService.handlePush(any(PushRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/sync/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.conflicts").isEmpty());
    }

    @Test
    void testPush_MissingUserId_ShouldReturnBadRequest() throws Exception {
        // Given
        PushRequest request = new PushRequest(null, Collections.emptyList());

        // When & Then
        mockMvc.perform(post("/api/sync/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPull_ValidRequest_ShouldReturnOk() throws Exception {
        // Given
        EntryDto entryDto = new EntryDto("id1", "{\"data\":\"test\"}", Instant.now(), false);
        PullResponse response = new PullResponse(Arrays.asList(entryDto), Instant.now());
        
        when(syncService.handlePull(any(String.class), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/sync/pull")
                .param("userId", "user123")
                .param("since", "2025-11-10T10:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries[0].id").value("id1"))
                .andExpect(jsonPath("$.serverTime").exists());
    }

    @Test
    void testPull_MissingUserId_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/sync/pull"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testHealth_ShouldReturnOk() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}
