package com.example.myapp.service;

import com.example.myapp.dto.Product;
import com.example.myapp.mapper.ProductMapper;
import com.example.myapp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    /**
     * Get all products
     */
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        log.debug("Fetching all products");
        return productRepository.findAll().stream()
            .map(productMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Get product by ID
     */
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        log.debug("Fetching product with id: {}", id);
        return productRepository.findById(id)
            .map(productMapper::toDto)
            .orElse(null);
    }

    /**
     * Get all products in stock
     */
    @Transactional(readOnly = true)
    public List<Product> getInStockProducts() {
        log.debug("Fetching in-stock products");
        return productRepository.findByInStockTrue().stream()
            .map(productMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Search products by name
     */
    @Transactional(readOnly = true)
    public List<Product> searchProductsByName(String name) {
        log.debug("Searching products with name containing: {}", name);
        return productRepository.findByNameContainingIgnoreCase(name).stream()
            .map(productMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Get products within price range
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsByPriceRange(double minPrice, double maxPrice) {
        log.debug("Fetching products between price {} and {}", minPrice, maxPrice);
        return productRepository.findByPriceBetween(minPrice, maxPrice).stream()
            .map(productMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Create new product
     */
    @Transactional
    public Product createProduct(Product productDto) {
        log.info("Creating new product: {}", productDto.name());
        com.example.myapp.model.Product entity = productMapper.toEntity(productDto);
        com.example.myapp.model.Product saved = productRepository.save(entity);
        return productMapper.toDto(saved);
    }

    /**
     * Update existing product
     */
    @Transactional
    public Product updateProduct(Long id, Product productDto) {
        log.info("Updating product with id: {}", id);
        return productRepository.findById(id)
            .map(entity -> {
                productMapper.updateEntity(entity, productDto);
                com.example.myapp.model.Product updated = productRepository.save(entity);
                return productMapper.toDto(updated);
            })
            .orElse(null);
    }

    /**
     * Delete product
     */
    @Transactional
    public boolean deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Update product stock status
     */
    @Transactional
    public Product updateStockStatus(Long id, boolean inStock) {
        log.info("Updating stock status for product {}: {}", id, inStock);
        return productRepository.findById(id)
            .map(entity -> {
                entity.setInStock(inStock);
                com.example.myapp.model.Product updated = productRepository.save(entity);
                return productMapper.toDto(updated);
            })
            .orElse(null);
    }
}
