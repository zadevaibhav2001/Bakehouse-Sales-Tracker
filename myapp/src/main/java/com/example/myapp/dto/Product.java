package com.example.myapp.dto;

import java.time.Instant;

public record Product(
    Long id,
    String name,
    double price,
    boolean inStock,
    Instant createdAt,
    Instant updatedAt
) {
}
