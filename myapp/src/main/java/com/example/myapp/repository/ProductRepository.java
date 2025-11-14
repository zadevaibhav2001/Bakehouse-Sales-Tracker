package com.example.myapp.repository;

import com.example.myapp.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find all products that are in stock
     */
    List<Product> findByInStockTrue();

    /**
     * Find all products that are out of stock
     */
    List<Product> findByInStockFalse();

    /**
     * Find products by name (case-insensitive)
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * Find products within a price range
     */
    List<Product> findByPriceBetween(double minPrice, double maxPrice);

    /**
     * Find products by exact name
     */
    Product findByName(String name);
}
