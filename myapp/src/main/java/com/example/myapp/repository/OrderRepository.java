package com.example.myapp.repository;

import com.example.myapp.model.Order;
import com.example.myapp.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find all orders for a specific product
     */
    List<Order> findByProduct(Product product);

    /**
     * Find all orders for a specific product ID
     */
    List<Order> findByProductId(Long productId);

    /**
     * Find orders placed after a specific date/time
     */
    List<Order> findByOrderDateTimeAfter(Instant dateTime);

    /**
     * Find orders placed between two dates
     */
    List<Order> findByOrderDateTimeBetween(Instant startDateTime, Instant endDateTime);

    /**
     * Find orders with quantity greater than specified amount
     */
    List<Order> findByQuantityGreaterThan(int quantity);

    /**
     * Get total quantity ordered for a specific product
     */
    @Query("SELECT SUM(o.quantity) FROM Order o WHERE o.product.id = :productId")
    Long getTotalQuantityByProductId(@Param("productId") Long productId);

    /**
     * Get all orders sorted by order date (most recent first)
     */
    List<Order> findAllByOrderByOrderDateTimeDesc();

    /**
     * Count orders for a specific product
     */
    long countByProductId(Long productId);
}
