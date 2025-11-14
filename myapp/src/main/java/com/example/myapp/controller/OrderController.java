package com.example.myapp.controller;

import com.example.myapp.dto.Order;
import com.example.myapp.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * Get all orders
     * GET /api/orders
     */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        log.info("GET /api/orders - Fetching all orders");
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Get recent orders (sorted by date, most recent first)
     * GET /api/orders/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Order>> getRecentOrders() {
        log.info("GET /api/orders/recent - Fetching recent orders");
        List<Order> orders = orderService.getRecentOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Get order by ID
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable UUID orderId) {
        log.info("GET /api/orders/{} - Fetching order", orderId);
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    /**
     * Get orders for a specific product
     * GET /api/orders/product/{productId}
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Order>> getOrdersByProduct(@PathVariable Long productId) {
        log.info("GET /api/orders/product/{} - Fetching orders for product", productId);
        List<Order> orders = orderService.getOrdersByProductId(productId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get orders after a specific date
     * GET /api/orders/after?date=2025-11-10T10:00:00
     */
    @GetMapping("/after")
    public ResponseEntity<List<Order>> getOrdersAfter(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        log.info("GET /api/orders/after?date={}", date);
        List<Order> orders = orderService.getOrdersAfter(date);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get orders within a date range
     * GET /api/orders/between?start=...&end=...
     */
    @GetMapping("/between")
    public ResponseEntity<List<Order>> getOrdersBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.info("GET /api/orders/between?start={}&end={}", start, end);
        List<Order> orders = orderService.getOrdersBetween(start, end);
        return ResponseEntity.ok(orders);
    }

    /**
     * Create new order
     * POST /api/orders
     * Body: { "product": { "name": "...", "price": 10.0, "inStock": true }, "quantity": 5 }
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        log.info("POST /api/orders - Creating order");
        try {
            Order created = orderService.createOrder(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Failed to create order: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create order with product ID
     * POST /api/orders/create
     * Body: { "productId": 1, "quantity": 5 }
     */
    @PostMapping("/create")
    public ResponseEntity<Order> createOrderWithProductId(
            @RequestBody Map<String, Object> request) {
        log.info("POST /api/orders/create - Creating order with product ID");
        try {
            Long productId = Long.valueOf(request.get("productId").toString());
            int quantity = Integer.parseInt(request.get("quantity").toString());
            
            Order created = orderService.createOrderWithProductId(productId, quantity);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Failed to create order: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Invalid request format", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update order quantity
     * PATCH /api/orders/{orderId}/quantity?quantity=10
     */
    @PatchMapping("/{orderId}/quantity")
    public ResponseEntity<Order> updateOrderQuantity(
            @PathVariable UUID orderId,
            @RequestParam int quantity) {
        log.info("PATCH /api/orders/{}/quantity?quantity={}", orderId, quantity);
        Order updated = orderService.updateOrderQuantity(orderId, quantity);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete order
     * DELETE /api/orders/{orderId}
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID orderId) {
        log.info("DELETE /api/orders/{}", orderId);
        boolean deleted = orderService.deleteOrder(orderId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Get statistics for a product
     * GET /api/orders/product/{productId}/stats
     */
    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<Map<String, Object>> getProductStats(@PathVariable Long productId) {
        log.info("GET /api/orders/product/{}/stats", productId);
        Long totalQuantity = orderService.getTotalQuantityForProduct(productId);
        long orderCount = orderService.countOrdersForProduct(productId);
        
        Map<String, Object> stats = Map.of(
            "productId", productId,
            "totalQuantityOrdered", totalQuantity,
            "totalOrders", orderCount
        );
        
        return ResponseEntity.ok(stats);
    }
}
