package com.example.myapp.mapper;

import com.example.myapp.dto.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class OrderMapper {

    private final ProductMapper productMapper;

    public OrderMapper(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /**
     * Convert Order entity to DTO
     */
    public Order toDto(com.example.myapp.model.Order entity) {
        if (entity == null) {
            return null;
        }
        return new Order(
            entity.getOrderId(),
            productMapper.toDto(entity.getProduct()),
            entity.getQuantity(),
            entity.getTotalCost(),
            entity.getOrderDateTime(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Convert Order DTO to entity
     * Note: Product must be fetched separately and set on the entity
     */
    public com.example.myapp.model.Order toEntity(Order dto, com.example.myapp.model.Product product) {
        if (dto == null) {
            return null;
        }
        com.example.myapp.model.Order entity = new com.example.myapp.model.Order();
        entity.setOrderId(dto.orderId());
        entity.setProduct(product);
        entity.setQuantity(dto.quantity());
        entity.setOrderDateTime(dto.orderDateTime());
        // totalCost will be calculated automatically in @PrePersist
        return entity;
    }

    /**
     * Update existing entity with DTO data
     */
    public void updateEntity(com.example.myapp.model.Order entity, Order dto, com.example.myapp.model.Product product) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setProduct(product);
        entity.setQuantity(dto.quantity());
        entity.setOrderDateTime(dto.orderDateTime());
        // totalCost will be recalculated automatically in @PreUpdate
    }
}
