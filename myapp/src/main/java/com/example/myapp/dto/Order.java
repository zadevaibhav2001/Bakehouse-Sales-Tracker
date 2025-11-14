package com.example.myapp.dto;

import java.time.Instant;
import java.util.UUID;

public record Order(
    UUID orderId, 
    Product product, 
    int quantity, 
    double totalCost,
    Instant orderDateTime,
    Instant createdAt,
    Instant updatedAt
) {
    public Order(Product product, int quantity) {
        this(UUID.randomUUID(), product, quantity, quantity * product.price(), Instant.now(), Instant.now(), Instant.now());
    }
}
