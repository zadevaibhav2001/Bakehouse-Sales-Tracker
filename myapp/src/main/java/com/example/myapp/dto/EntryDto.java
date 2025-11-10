package com.example.myapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntryDto {
    private String id;
    private String payload;
    private Instant updatedAt;
    private boolean deleted;
}
