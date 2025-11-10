package com.example.myapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "entries", indexes = {
    @Index(name = "idx_entries_user_updated", columnList = "userId,updatedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Entry {

    @Id
    @Column(length = 36)
    private String id;  // Client-generated UUID

    @Column(nullable = false)
    private String userId;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;  // JSON blob for flexible data storage

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deleted = false;
}
