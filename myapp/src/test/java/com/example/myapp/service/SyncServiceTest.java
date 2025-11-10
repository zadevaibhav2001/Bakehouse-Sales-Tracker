package com.example.myapp.service;

import com.example.myapp.dto.*;
import com.example.myapp.model.Entry;
import com.example.myapp.repository.EntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private EntryRepository entryRepository;

    @InjectMocks
    private SyncService syncService;

    private String userId;
    private String entryId;

    @BeforeEach
    void setUp() {
        userId = "user123";
        entryId = "entry-uuid-1";
    }

    @Test
    void testHandlePush_NewEntry_ShouldInsert() {
        // Given
        EntryDto entryDto = new EntryDto(entryId, "{\"data\":\"test\"}", Instant.now(), false);
        PushRequest request = new PushRequest(userId, Arrays.asList(entryDto));
        
        when(entryRepository.findById(entryId)).thenReturn(Optional.empty());
        when(entryRepository.save(any(Entry.class))).thenAnswer(i -> i.getArgument(0));

        // When
        PushResponse response = syncService.handlePush(request);

        // Then
        assertEquals(1, response.getAccepted());
        assertEquals(0, response.getConflicts().size());
        verify(entryRepository, times(1)).save(any(Entry.class));
    }

    @Test
    void testHandlePush_ClientVersionNewer_ShouldUpdate() {
        // Given
        Instant serverTime = Instant.parse("2025-11-10T10:00:00Z");
        Instant clientTime = Instant.parse("2025-11-10T11:00:00Z");
        
        Entry serverEntry = new Entry(entryId, userId, "{\"data\":\"old\"}", serverTime, false);
        EntryDto clientDto = new EntryDto(entryId, "{\"data\":\"new\"}", clientTime, false);
        PushRequest request = new PushRequest(userId, Arrays.asList(clientDto));
        
        when(entryRepository.findById(entryId)).thenReturn(Optional.of(serverEntry));
        when(entryRepository.save(any(Entry.class))).thenAnswer(i -> i.getArgument(0));

        // When
        PushResponse response = syncService.handlePush(request);

        // Then
        assertEquals(1, response.getAccepted());
        assertEquals(0, response.getConflicts().size());
        verify(entryRepository, times(1)).save(any(Entry.class));
    }

    @Test
    void testHandlePush_ServerVersionNewer_ShouldConflict() {
        // Given
        Instant serverTime = Instant.parse("2025-11-10T11:00:00Z");
        Instant clientTime = Instant.parse("2025-11-10T10:00:00Z");
        
        Entry serverEntry = new Entry(entryId, userId, "{\"data\":\"server\"}", serverTime, false);
        EntryDto clientDto = new EntryDto(entryId, "{\"data\":\"client\"}", clientTime, false);
        PushRequest request = new PushRequest(userId, Arrays.asList(clientDto));
        
        when(entryRepository.findById(entryId)).thenReturn(Optional.of(serverEntry));

        // When
        PushResponse response = syncService.handlePush(request);

        // Then
        assertEquals(0, response.getAccepted());
        assertEquals(1, response.getConflicts().size());
        assertEquals(entryId, response.getConflicts().get(0).getId());
        assertEquals("Server version is newer", response.getConflicts().get(0).getReason());
        verify(entryRepository, never()).save(any(Entry.class));
    }

    @Test
    void testHandlePush_SameTimestamp_ShouldAccept() {
        // Given
        Instant timestamp = Instant.parse("2025-11-10T10:00:00Z");
        
        Entry serverEntry = new Entry(entryId, userId, "{\"data\":\"same\"}", timestamp, false);
        EntryDto clientDto = new EntryDto(entryId, "{\"data\":\"same\"}", timestamp, false);
        PushRequest request = new PushRequest(userId, Arrays.asList(clientDto));
        
        when(entryRepository.findById(entryId)).thenReturn(Optional.of(serverEntry));

        // When
        PushResponse response = syncService.handlePush(request);

        // Then
        assertEquals(1, response.getAccepted());
        assertEquals(0, response.getConflicts().size());
    }

    @Test
    void testHandlePull_WithSinceTimestamp_ShouldReturnFilteredEntries() {
        // Given
        Instant since = Instant.parse("2025-11-10T10:00:00Z");
        Entry entry1 = new Entry("id1", userId, "{}", Instant.parse("2025-11-10T11:00:00Z"), false);
        Entry entry2 = new Entry("id2", userId, "{}", Instant.parse("2025-11-10T12:00:00Z"), false);
        
        when(entryRepository.findByUserIdAndUpdatedAtAfter(userId, since))
            .thenReturn(Arrays.asList(entry1, entry2));

        // When
        PullResponse response = syncService.handlePull(userId, since.toString());

        // Then
        assertEquals(2, response.getEntries().size());
        assertNotNull(response.getServerTime());
        verify(entryRepository, times(1)).findByUserIdAndUpdatedAtAfter(userId, since);
    }

    @Test
    void testHandlePull_WithoutSinceTimestamp_ShouldReturnAllEntries() {
        // Given
        Entry entry1 = new Entry("id1", userId, "{}", Instant.now(), false);
        Entry entry2 = new Entry("id2", userId, "{}", Instant.now(), false);
        
        when(entryRepository.findByUserId(userId))
            .thenReturn(Arrays.asList(entry1, entry2));

        // When
        PullResponse response = syncService.handlePull(userId, null);

        // Then
        assertEquals(2, response.getEntries().size());
        assertNotNull(response.getServerTime());
        verify(entryRepository, times(1)).findByUserId(userId);
    }
}
