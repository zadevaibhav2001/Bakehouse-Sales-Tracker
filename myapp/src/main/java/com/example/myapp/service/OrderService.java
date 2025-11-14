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
        
        com.example.myapp.model.Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

        com.example.myapp.model.Order entity = new com.example.myapp.model.Order();
        entity.setOrderId(UUID.randomUUID());
        entity.setProduct(product);
        entity.setQuantity(quantity);
        entity.setOrderDateTime(Instant.now());

        com.example.myapp.model.Order saved = orderRepository.save(entity);
        return orderMapper.toDto(saved);
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
                com.example.myapp.model.Order updated = orderRepository.save(entity);
                return orderMapper.toDto(updated);
            })
            .orElse(null);
    }

    /**
     * Delete order
     */
    @Transactional
    public boolean deleteOrder(UUID orderId) {
        log.info("Deleting order with id: {}", orderId);
        if (orderRepository.existsById(orderId)) {
            orderRepository.deleteById(orderId);
            return true;
        }
        return false;
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
}
