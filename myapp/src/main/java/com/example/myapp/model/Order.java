package com.example.myapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_order_date", columnList = "orderDateTime"),
    @Index(name = "idx_orders_product", columnList = "product_id")
})
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
    private double totalCost;

    @Column(nullable = false)
    private Instant orderDateTime;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Order() {}

    public Order(UUID orderId, Product product, int quantity, double totalCost, Instant orderDateTime, Instant createdAt, Instant updatedAt) {
        this.orderId = orderId;
        this.product = product;
        this.quantity = quantity;
        this.totalCost = totalCost;
        this.orderDateTime = orderDateTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
    
    public Instant getOrderDateTime() { return orderDateTime; }
    public void setOrderDateTime(Instant orderDateTime) { this.orderDateTime = orderDateTime; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        if (orderId == null) {
            orderId = UUID.randomUUID();
        }
        if (orderDateTime == null) {
            orderDateTime = Instant.now();
        }
        if (product != null) {
            totalCost = quantity * product.getPrice();
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (product != null) {
            totalCost = quantity * product.getPrice();
        }
        updatedAt = Instant.now();
    }
}
