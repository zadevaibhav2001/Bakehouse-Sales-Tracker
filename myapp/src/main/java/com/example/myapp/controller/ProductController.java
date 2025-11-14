package com.example.myapp.controller;

import com.example.myapp.dto.Product;
import com.example.myapp.service.ExcelExportService;
import com.example.myapp.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final ExcelExportService excelExportService;

    /**
     * Get all products
     * GET /api/products
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        log.info("GET /api/products - Fetching all products");
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Get product by ID
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        log.info("GET /api/products/{} - Fetching product", id);
        Product product = productService.getProductById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    /**
     * Get all in-stock products
     * GET /api/products/in-stock
     */
    @GetMapping("/in-stock")
    public ResponseEntity<List<Product>> getInStockProducts() {
        log.info("GET /api/products/in-stock - Fetching in-stock products");
        List<Product> products = productService.getInStockProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Search products by name
     * GET /api/products/search?name=...
     */
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String name) {
        log.info("GET /api/products/search?name={}", name);
        List<Product> products = productService.searchProductsByName(name);
        return ResponseEntity.ok(products);
    }

    /**
     * Get products by price range
     * GET /api/products/price-range?min=...&max=...
     */
    @GetMapping("/price-range")
    public ResponseEntity<List<Product>> getProductsByPriceRange(
            @RequestParam double min,
            @RequestParam double max) {
        log.info("GET /api/products/price-range?min={}&max={}", min, max);
        List<Product> products = productService.getProductsByPriceRange(min, max);
        return ResponseEntity.ok(products);
    }

    /**
     * Create new product
     * POST /api/products
     */
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        log.info("POST /api/products - Creating product: {}", product.name());
        try {
            Product created = productService.createProduct(product);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Cannot create product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update product
     * PUT /api/products/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody Product product) {
        log.info("PUT /api/products/{} - Updating product", id);
        Product updated = productService.updateProduct(id, product);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /**
     * Update product stock status
     * PATCH /api/products/{id}/stock?inStock=true
     */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> updateStockStatus(
            @PathVariable Long id,
            @RequestParam boolean inStock) {
        log.info("PATCH /api/products/{}/stock?inStock={}", id, inStock);
        Product updated = productService.updateStockStatus(id, inStock);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete product
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        log.info("DELETE /api/products/{}", id);
        try {
            boolean deleted = productService.deleteProduct(id);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Cannot delete product {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Simple error response record
     */
    private record ErrorResponse(String message) {}

    /**
     * Export all products to Excel
     * GET /api/products/export/excel
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportProductsToExcel() {
        log.info("GET /api/products/export/excel - Exporting all products");
        try {
            List<Product> products = productService.getAllProducts();
            byte[] excelData = excelExportService.generateProductsExcel(products);
            
            String filename = "products_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            
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
     * Export in-stock products to Excel
     * GET /api/products/export/excel/in-stock
     */
    @GetMapping("/export/excel/in-stock")
    public ResponseEntity<byte[]> exportInStockProductsToExcel() {
        log.info("GET /api/products/export/excel/in-stock - Exporting in-stock products");
        try {
            List<Product> products = productService.getInStockProducts();
            byte[] excelData = excelExportService.generateProductsExcel(products);
            
            String filename = "products_in_stock_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            
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
