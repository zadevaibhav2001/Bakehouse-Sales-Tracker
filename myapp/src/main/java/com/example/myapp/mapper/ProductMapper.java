package com.example.myapp.mapper;

import com.example.myapp.dto.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    /**
     * Convert Product entity to DTO
     */
    public Product toDto(com.example.myapp.model.Product entity) {
        if (entity == null) {
            return null;
        }
        return new Product(
            entity.getId(),
            entity.getName(),
            entity.getPrice(),
            entity.isInStock(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Convert Product DTO to entity
     */
    public com.example.myapp.model.Product toEntity(Product dto) {
        if (dto == null) {
            return null;
        }
        com.example.myapp.model.Product entity = new com.example.myapp.model.Product();
        entity.setName(dto.name());
        entity.setPrice(dto.price());
        entity.setInStock(dto.inStock());
        return entity;
    }

    /**
     * Update existing entity with DTO data
     */
    public void updateEntity(com.example.myapp.model.Product entity, Product dto) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setName(dto.name());
        entity.setPrice(dto.price());
        entity.setInStock(dto.inStock());
    }
}
