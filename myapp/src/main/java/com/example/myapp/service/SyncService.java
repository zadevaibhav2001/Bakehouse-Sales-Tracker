package com.example.myapp.service;

import com.example.myapp.dto.*;
import com.example.myapp.model.Entry;
import com.example.myapp.repository.EntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private final EntryRepository entryRepository;

    /**
     * Handle push sync from mobile client
     * Implements last-write-wins conflict resolution based on updatedAt timestamps
     */
    @Transactional
    public PushResponse handlePush(PushRequest request) {
        log.info("Processing push sync for user: {} with {} entries", 
                 request.getUserId(), request.getEntries().size());

        int acceptedCount = 0;
        List<ConflictRecord> conflicts = new ArrayList<>();

        for (EntryDto entryDto : request.getEntries()) {
            Optional<Entry> existingEntry = entryRepository.findById(entryDto.getId());

            if (existingEntry.isEmpty()) {
                // New entry - insert
                Entry newEntry = convertToEntity(entryDto, request.getUserId());
                entryRepository.save(newEntry);
                acceptedCount++;
                log.debug("Inserted new entry: {}", entryDto.getId());
            } else {
                // Existing entry - check timestamp for conflict resolution
                Entry serverEntry = existingEntry.get();
                
                if (entryDto.getUpdatedAt().isAfter(serverEntry.getUpdatedAt())) {
                    // Client version is newer - accept update
                    serverEntry.setPayload(entryDto.getPayload());
                    serverEntry.setUpdatedAt(entryDto.getUpdatedAt());
                    serverEntry.setDeleted(entryDto.isDeleted());
                    entryRepository.save(serverEntry);
                    acceptedCount++;
                    log.debug("Updated entry (client newer): {}", entryDto.getId());
                } else if (entryDto.getUpdatedAt().isBefore(serverEntry.getUpdatedAt())) {
                    // Server version is newer - conflict
                    ConflictRecord conflict = new ConflictRecord(
                        entryDto.getId(),
                        convertToDto(serverEntry),
                        "Server version is newer"
                    );
                    conflicts.add(conflict);
                    log.debug("Conflict detected for entry: {} (server version newer)", entryDto.getId());
                } else {
                    // Same timestamp - accept (idempotent)
                    acceptedCount++;
                    log.debug("Entry unchanged (same timestamp): {}", entryDto.getId());
                }
            }
        }

        log.info("Push sync completed: {} accepted, {} conflicts", acceptedCount, conflicts.size());
        return new PushResponse(acceptedCount, conflicts);
    }

    /**
     * Handle pull sync from mobile client
     * Returns all entries modified after the given timestamp
     */
    @Transactional(readOnly = true)
    public PullResponse handlePull(String userId, String sinceStr) {
        log.info("Processing pull sync for user: {} since: {}", userId, sinceStr);

        List<Entry> entries;
        
        if (sinceStr != null && !sinceStr.isEmpty()) {
            Instant since = Instant.parse(sinceStr);
            entries = entryRepository.findByUserIdAndUpdatedAtAfter(userId, since);
            log.debug("Found {} entries updated after {}", entries.size(), since);
        } else {
            entries = entryRepository.findByUserId(userId);
            log.debug("Found {} total entries for user", entries.size());
        }

        List<EntryDto> entryDtos = entries.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

        Instant serverTime = Instant.now();
        log.info("Pull sync completed: {} entries returned", entryDtos.size());
        
        return new PullResponse(entryDtos, serverTime);
    }

    /**
     * Convert DTO to Entity
     */
    private Entry convertToEntity(EntryDto dto, String userId) {
        Entry entry = new Entry();
        entry.setId(dto.getId());
        entry.setUserId(userId);
        entry.setPayload(dto.getPayload());
        entry.setUpdatedAt(dto.getUpdatedAt());
        entry.setDeleted(dto.isDeleted());
        return entry;
    }

    /**
     * Convert Entity to DTO
     */
    private EntryDto convertToDto(Entry entry) {
        return new EntryDto(
            entry.getId(),
            entry.getPayload(),
            entry.getUpdatedAt(),
            entry.isDeleted()
        );
    }
}
