package com.example.myapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_order_date", columnList = "orderDateTime"),
    @Index(name = "idx_orders_product", columnList = "product_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private Instant orderDateTime;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (orderId == null) {
            orderId = UUID.randomUUID();
        }
        if (orderDateTime == null) {
            orderDateTime = Instant.now();
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
