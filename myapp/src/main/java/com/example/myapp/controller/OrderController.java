package com.example.myapp.controller;

import com.example.myapp.dto.Order;
import com.example.myapp.service.ExcelExportService;
import com.example.myapp.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final ExcelExportService excelExportService;

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
    public ResponseEntity<?> createOrderWithProductId(
            @RequestBody Map<String, Object> request) {
        log.info("POST /api/orders/create - Creating order with product ID: {}", request);
        try {
            if (!request.containsKey("productId") || !request.containsKey("quantity")) {
                log.error("Missing required fields: productId or quantity");
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields: productId and quantity"));
            }
            
            Long productId = Long.valueOf(request.get("productId").toString());
            int quantity = Integer.parseInt(request.get("quantity").toString());
            
            log.info("Creating order for productId: {} with quantity: {}", productId, quantity);
            Order created = orderService.createOrderWithProductId(productId, quantity);
            log.info("Order created successfully: {}", created.orderId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Failed to create order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Invalid request format or internal error", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request format: " + e.getMessage()));
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
     * DELETE /api/orders/{orderId}?forceDelete=true
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> deleteOrder(
            @PathVariable UUID orderId,
            @RequestParam(defaultValue = "false") boolean forceDelete) {
        log.info("DELETE /api/orders/{}?forceDelete={}", orderId, forceDelete);
        try {
            boolean deleted = orderService.deleteOrder(orderId, forceDelete);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Order deletion blocked: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
        }
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
        double totalRevenue = orderService.getTotalRevenueForProduct(productId);
        
        Map<String, Object> stats = Map.of(
            "productId", productId,
            "totalQuantityOrdered", totalQuantity,
            "totalOrders", orderCount,
            "totalRevenue", totalRevenue
        );
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get total revenue from all orders
     * GET /api/orders/revenue/total
     */
    @GetMapping("/revenue/total")
    public ResponseEntity<Map<String, Object>> getTotalRevenue() {
        log.info("GET /api/orders/revenue/total");
        double totalRevenue = orderService.getTotalRevenue();
        return ResponseEntity.ok(Map.of("totalRevenue", totalRevenue));
    }

    /**
     * Get revenue within a date range
     * GET /api/orders/revenue/between?start=...&end=...
     */
    @GetMapping("/revenue/between")
    public ResponseEntity<Map<String, Object>> getRevenueBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.info("GET /api/orders/revenue/between?start={}&end={}", start, end);
        double revenue = orderService.getRevenueBetween(start, end);
        return ResponseEntity.ok(Map.of(
            "startDate", start,
            "endDate", end,
            "totalRevenue", revenue
        ));
    }

    /**
     * Get high-value orders (above specified amount)
     * GET /api/orders/high-value?minCost=100
     */
    @GetMapping("/high-value")
    public ResponseEntity<List<Order>> getHighValueOrders(@RequestParam double minCost) {
        log.info("GET /api/orders/high-value?minCost={}", minCost);
        List<Order> orders = orderService.getHighValueOrders(minCost);
        return ResponseEntity.ok(orders);
    }

    /**
     * Export all orders to Excel
     * GET /api/orders/export/excel
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportOrdersToExcel() {
        log.info("GET /api/orders/export/excel - Exporting all orders");
        try {
            List<Order> orders = orderService.getAllOrders();
            byte[] excelData = excelExportService.generateOrdersExcel(orders);
            
            String filename = "orders_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            log.error("Failed to generate Excel file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export orders for a specific product to Excel
     * GET /api/orders/export/excel/product/{productId}
     */
    @GetMapping("/export/excel/product/{productId}")
    public ResponseEntity<byte[]> exportProductOrdersToExcel(@PathVariable Long productId) {
        log.info("GET /api/orders/export/excel/product/{} - Exporting orders for product", productId);
        try {
            List<Order> orders = orderService.getOrdersByProductId(productId);
            byte[] excelData = excelExportService.generateOrdersExcel(orders);
            
            String filename = "orders_product_" + productId + "_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            log.error("Failed to generate Excel file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export orders within date range to Excel
     * GET /api/orders/export/excel/between?start=...&end=...
     */
    @GetMapping("/export/excel/between")
    public ResponseEntity<byte[]> exportOrdersBetweenToExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.info("GET /api/orders/export/excel/between?start={}&end={}", start, end);
        try {
            List<Order> orders = orderService.getOrdersBetween(start, end);
            byte[] excelData = excelExportService.generateOrdersExcel(orders);
            
            String filename = "orders_" + start.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + 
                    "_to_" + end.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            log.error("Failed to generate Excel file", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
