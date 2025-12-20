package com.example.myapp.service;

import com.example.myapp.dto.Order;
import com.example.myapp.mapper.OrderMapper;
import com.example.myapp.repository.OrderRepository;
import com.example.myapp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    /**
     * Get all orders
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        log.debug("Fetching all orders");
        return orderRepository.findAll().stream()
            .map(orderMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Get order by ID
     */
    @Transactional(readOnly = true)
    public Order getOrderById(UUID orderId) {
        log.debug("Fetching order with id: {}", orderId);
        return orderRepository.findById(orderId)
            .map(orderMapper::toDto)
            .orElse(null);
    }

    /**
     * Get all orders sorted by date (most recent first)
     */
    @Transactional(readOnly = true)
    public List<Order> getRecentOrders() {
        log.debug("Fetching recent orders");
        return orderRepository.findAllByOrderByOrderDateTimeDesc().stream()
            .map(orderMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Get orders for a specific product
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByProductId(Long productId) {
        log.debug("Fetching orders for product: {}", productId);
        return orderRepository.findByProductId(productId).stream()
            .map(orderMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Get orders placed after a specific date
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersAfter(LocalDateTime dateTime) {
        log.debug("Fetching orders after: {}", dateTime);
        Instant instant = dateTime.toInstant(ZoneOffset.UTC);
        return orderRepository.findByOrderDateTimeAfter(instant).stream()
            .map(orderMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Get orders within a date range
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        log.debug("Fetching orders between {} and {}", startDateTime, endDateTime);
        Instant start = startDateTime.toInstant(ZoneOffset.UTC);
        Instant end = endDateTime.toInstant(ZoneOffset.UTC);
        return orderRepository.findByOrderDateTimeBetween(start, end).stream()
            .map(orderMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Create new order
     */
    @Transactional
    public Order createOrder(Order orderDto) {
        log.info("Creating new order for product: {}", orderDto.product().name());
        
        // Find product by name (you might want to use ID instead)
        com.example.myapp.model.Product product = productRepository.findByName(orderDto.product().name());
        if (product == null) {
            log.error("Product not found: {}", orderDto.product().name());
            throw new IllegalArgumentException("Product not found: " + orderDto.product().name());
        }

        com.example.myapp.model.Order entity = orderMapper.toEntity(orderDto, product);
        com.example.myapp.model.Order saved = orderRepository.save(entity);
        return orderMapper.toDto(saved);
    }

    /**
     * Create order with product ID
     */
    @Transactional
    public Order createOrderWithProductId(Long productId, int quantity) {
        log.info("Creating new order for product ID: {} with quantity: {}", productId, quantity);
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        
        com.example.myapp.model.Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));
        
        log.info("Found product: {} with price: {}", product.getName(), product.getPrice());

        com.example.myapp.model.Order entity = new com.example.myapp.model.Order();
        entity.setOrderId(UUID.randomUUID());
        entity.setProduct(product);
        entity.setQuantity(quantity);
        entity.setTotalCost(quantity * product.getPrice());
        entity.setOrderDateTime(Instant.now());
        
        log.info("Saving order with totalCost: {}", entity.getTotalCost());
        com.example.myapp.model.Order saved = orderRepository.save(entity);
        log.info("Order saved with ID: {}", saved.getOrderId());
        
        Order dto = orderMapper.toDto(saved);
        log.info("Returning order DTO: {}", dto);
        return dto;
    }

    /**
     * Update order quantity
     */
    @Transactional
    public Order updateOrderQuantity(UUID orderId, int newQuantity) {
        log.info("Updating order {} quantity to: {}", orderId, newQuantity);
        return orderRepository.findById(orderId)
            .map(entity -> {
                entity.setQuantity(newQuantity);
                entity.setTotalCost(newQuantity * entity.getProduct().getPrice());
                return orderMapper.toDto(orderRepository.save(entity));
            })
            .orElse(null);
    }

    @Transactional
    public Order updateOrder(UUID orderId, Order orderDto) {
        return orderRepository.findById(orderId)
            .map(entity -> {
                if (orderDto.orderDateTime() != null) {
                    entity.setOrderDateTime(orderDto.orderDateTime());
                }
                if (orderDto.quantity() > 0) {
                    entity.setQuantity(orderDto.quantity());
                    entity.setTotalCost(orderDto.quantity() * entity.getProduct().getPrice());
                }
                return orderMapper.toDto(orderRepository.save(entity));
            })
            .orElse(null);
    }

    /**
     * Delete order
     */
    @Transactional
    public boolean deleteOrder(UUID orderId) {
        return deleteOrder(orderId, false);
    }

    /**
     * Delete order with force flag
     */
    @Transactional
    public boolean deleteOrder(UUID orderId, boolean forceDelete) {
        log.info("Deleting order with id: {}, forceDelete: {}", orderId, forceDelete);
        
        return orderRepository.findById(orderId)
            .map(order -> {
                if (!forceDelete) {
                    Instant oneHourAgo = Instant.now().minusSeconds(3600);
                    if (order.getCreatedAt().isBefore(oneHourAgo)) {
                        log.warn("Cannot delete order {} - created more than 1 hour ago", orderId);
                        throw new IllegalStateException("Cannot delete order created more than 1 hour ago");
                    }
                }
                orderRepository.deleteById(orderId);
                log.info("Order {} deleted successfully", orderId);
                return true;
            })
            .orElse(false);
    }

    /**
     * Get total quantity ordered for a product
     */
    @Transactional(readOnly = true)
    public Long getTotalQuantityForProduct(Long productId) {
        log.debug("Calculating total quantity for product: {}", productId);
        Long total = orderRepository.getTotalQuantityByProductId(productId);
        return total != null ? total : 0L;
    }

    /**
     * Count orders for a product
     */
    @Transactional(readOnly = true)
    public long countOrdersForProduct(Long productId) {
        log.debug("Counting orders for product: {}", productId);
        return orderRepository.countByProductId(productId);
    }

    /**
     * Get total revenue from all orders
     */
    @Transactional(readOnly = true)
    public double getTotalRevenue() {
        log.debug("Calculating total revenue");
        Double total = orderRepository.getTotalRevenue();
        return total != null ? total : 0.0;
    }

    /**
     * Get total revenue for a specific product
     */
    @Transactional(readOnly = true)
    public double getTotalRevenueForProduct(Long productId) {
        log.debug("Calculating total revenue for product: {}", productId);
        Double total = orderRepository.getTotalRevenueByProductId(productId);
        return total != null ? total : 0.0;
    }

    /**
     * Get total revenue within a date range
     */
    @Transactional(readOnly = true)
    public double getRevenueBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        log.debug("Calculating revenue between {} and {}", startDateTime, endDateTime);
        Instant start = startDateTime.toInstant(ZoneOffset.UTC);
        Instant end = endDateTime.toInstant(ZoneOffset.UTC);
        Double total = orderRepository.getTotalRevenueBetween(start, end);
        return total != null ? total : 0.0;
    }

    /**
     * Get high-value orders (above specified amount)
     */
    @Transactional(readOnly = true)
    public List<Order> getHighValueOrders(double minCost) {
        log.debug("Fetching orders with cost greater than: {}", minCost);
        return orderRepository.findByTotalCostGreaterThan(minCost).stream()
            .map(orderMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Get count of orders for today
     */
    @Transactional(readOnly = true)
    public long getTodaysOrderCount() {
        log.debug("Counting today's orders");
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        
        Instant startInstant = startOfDay.toInstant(ZoneOffset.UTC);
        Instant endInstant = endOfDay.toInstant(ZoneOffset.UTC);
        
        return orderRepository.countOrdersForToday(startInstant, endInstant);
    }
}
