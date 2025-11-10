package com.example.myapp.repository;

import com.example.myapp.model.Entry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EntryRepository extends JpaRepository<Entry, String> {

    /**
     * Find all entries for a user that were updated after a specific timestamp
     * Used for pull sync operations
     */
    List<Entry> findByUserIdAndUpdatedAtAfter(String userId, Instant since);

    /**
     * Find all entries for a specific user
     */
    List<Entry> findByUserId(String userId);
}
