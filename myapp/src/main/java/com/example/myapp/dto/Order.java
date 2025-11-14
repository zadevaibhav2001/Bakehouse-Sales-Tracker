package com.example.myapp.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record Order(UUID orderId, Product product, int quantity, LocalDateTime orderDateTime) {
    public Order(Product product, int quantity) {
        this(UUID.randomUUID(), product, quantity, LocalDateTime.now());
    }
}
