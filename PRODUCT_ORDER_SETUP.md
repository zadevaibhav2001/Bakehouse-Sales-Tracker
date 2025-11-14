# Product and Order Tables Setup

## What Has Been Created

I've created a complete Product and Order management system based on your DTOs.

### 📦 Created Files

#### 1. **Entity Models** (`myapp/src/main/java/com/example/myapp/model/`)
- ✅ `Product.java` - JPA entity with auto-generated ID, timestamps
- ✅ `Order.java` - JPA entity with UUID, foreign key to Product, indexes

#### 2. **Repositories** (`myapp/src/main/java/com/example/myapp/repository/`)
- ✅ `ProductRepository.java` - CRUD + custom queries:
  - Find by stock status
  - Search by name
  - Find by price range
  
- ✅ `OrderRepository.java` - CRUD + custom queries:
  - Find by product
  - Find by date range
  - Calculate total quantities
  - Count orders

#### 3. **Mappers** (`myapp/src/main/java/com/example/myapp/mapper/`)
- ✅ `ProductMapper.java` - Convert between DTO and Entity
- ✅ `OrderMapper.java` - Convert between DTO and Entity

#### 4. **Services** (`myapp/src/main/java/com/example/myapp/service/`)
- ✅ `ProductService.java` - Business logic for products
- ✅ `OrderService.java` - Business logic for orders

#### 5. **Controllers** (`myapp/src/main/java/com/example/myapp/controller/`)
- ✅ `ProductController.java` - REST API for products
- ✅ `OrderController.java` - REST API for orders

#### 6. **Database Schema** (`myapp/src/main/resources/schema.sql`)
- ✅ Updated with `products` and `orders` tables
- ✅ Indexes for performance
- ✅ Foreign key constraints

---

## 📊 Database Schema

### Products Table
```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    in_stock BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

### Orders Table
```sql
CREATE TABLE orders (
    order_id UUID PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    order_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
```

---

## 🔌 API Endpoints

### Product Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products` | Get all products |
| GET | `/api/products/{id}` | Get product by ID |
| GET | `/api/products/in-stock` | Get in-stock products |
| GET | `/api/products/search?name=...` | Search products by name |
| GET | `/api/products/price-range?min=...&max=...` | Get products by price range |
| POST | `/api/products` | Create new product |
| PUT | `/api/products/{id}` | Update product |
| PATCH | `/api/products/{id}/stock?inStock=true` | Update stock status |
| DELETE | `/api/products/{id}` | Delete product |

### Order Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/orders` | Get all orders |
| GET | `/api/orders/recent` | Get recent orders (sorted) |
| GET | `/api/orders/{orderId}` | Get order by ID |
| GET | `/api/orders/product/{productId}` | Get orders for a product |
| GET | `/api/orders/after?date=...` | Get orders after date |
| GET | `/api/orders/between?start=...&end=...` | Get orders in date range |
| GET | `/api/orders/product/{productId}/stats` | Get product statistics |
| POST | `/api/orders` | Create order (with product DTO) |
| POST | `/api/orders/create` | Create order (with product ID) |
| PATCH | `/api/orders/{orderId}/quantity?quantity=...` | Update order quantity |
| DELETE | `/api/orders/{orderId}` | Delete order |

---

## 🚀 How to Use

### 1. Initialize Database

```bash
# SSH into your EC2 instance
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP>

# Run the updated schema
PGPASSWORD='<DB_PASSWORD>' psql -U myappuser -d myappdb -h localhost -f /path/to/schema.sql

# Or manually:
PGPASSWORD='<DB_PASSWORD>' psql -U myappuser -d myappdb -h localhost <<EOF
-- Paste the contents of schema.sql here
EOF
```

### 2. Build and Deploy

```bash
# Build the application
cd myapp
mvn clean package

# Deploy
cd ..
./scripts/deploy.sh
```

### 3. Test the APIs

#### Create a Product
```bash
curl -X POST http://<YOUR_IP>/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "price": 999.99,
    "inStock": true
  }'
```

#### Get All Products
```bash
curl http://<YOUR_IP>/api/products
```

#### Create an Order (Method 1: with product ID)
```bash
curl -X POST http://<YOUR_IP>/api/orders/create \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 5
  }'
```

#### Create an Order (Method 2: with product DTO)
```bash
curl -X POST http://<YOUR_IP>/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "product": {
      "name": "Laptop",
      "price": 999.99,
      "inStock": true
    },
    "quantity": 3
  }'
```

#### Get All Orders
```bash
curl http://<YOUR_IP>/api/orders
```

#### Get Product Statistics
```bash
curl http://<YOUR_IP>/api/orders/product/1/stats
```

#### Search Products
```bash
curl "http://<YOUR_IP>/api/products/search?name=laptop"
```

#### Get Orders After Date
```bash
curl "http://<YOUR_IP>/api/orders/after?date=2025-11-10T00:00:00"
```

---

## 📝 Example Usage Flow

### 1. Create Some Products
```bash
# Product 1
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop", "price": 999.99, "inStock": true}'

# Product 2
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Mouse", "price": 29.99, "inStock": true}'

# Product 3
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Keyboard", "price": 79.99, "inStock": false}'
```

### 2. Create Orders
```bash
# Order for Laptop
curl -X POST http://localhost:8080/api/orders/create \
  -H "Content-Type: application/json" \
  -d '{"productId": 1, "quantity": 2}'

# Order for Mouse
curl -X POST http://localhost:8080/api/orders/create \
  -H "Content-Type: application/json" \
  -d '{"productId": 2, "quantity": 10}'
```

### 3. Query Data
```bash
# Get all in-stock products
curl http://localhost:8080/api/products/in-stock

# Get recent orders
curl http://localhost:8080/api/orders/recent

# Get statistics for product 1
curl http://localhost:8080/api/orders/product/1/stats
```

---

## 🔧 Key Features

### Product Features
- ✅ CRUD operations
- ✅ Stock management
- ✅ Price range filtering
- ✅ Name search (case-insensitive)
- ✅ Automatic timestamps

### Order Features
- ✅ CRUD operations
- ✅ UUID-based IDs
- ✅ Product relationship (foreign key)
- ✅ Date range queries
- ✅ Product statistics (total quantity, order count)
- ✅ Automatic timestamps
- ✅ Cascade delete (deleting product deletes its orders)

### Technical Features
- ✅ JPA entities with proper relationships
- ✅ Repository pattern with custom queries
- ✅ Service layer for business logic
- ✅ DTO to Entity mapping
- ✅ REST API with proper HTTP methods
- ✅ Logging throughout
- ✅ Transaction management
- ✅ Error handling

---

## 🎯 What's Different from Your DTOs

Your DTOs use Java records, which are immutable. The entities I created:

1. **Product Entity**:
   - Added `id` (auto-generated)
   - Added `createdAt` and `updatedAt` timestamps
   - Uses `@Entity` for JPA

2. **Order Entity**:
   - Uses `UUID` for `orderId` (same as your DTO)
   - Changed `Product` from embedded to `@ManyToOne` relationship
   - Added `createdAt` and `updatedAt` timestamps
   - Changed `LocalDateTime` to `Instant` for better timezone handling

3. **Mappers**:
   - Convert between your immutable DTOs and mutable entities
   - Handle timestamp conversions

---

## 📚 Next Steps

### 1. Add Validation
```java
// In Product DTO or entity
@NotBlank(message = "Product name is required")
private String name;

@Positive(message = "Price must be positive")
private double price;
```

### 2. Add Pagination
```java
// In ProductRepository
Page<Product> findAll(Pageable pageable);

// In ProductController
@GetMapping
public ResponseEntity<Page<Product>> getAllProducts(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    // ...
}
```

### 3. Add Authentication
- Secure endpoints with Spring Security
- Add user ownership to orders
- Implement role-based access control

### 4. Add More Business Logic
- Inventory management (reduce stock on order)
- Order status (pending, completed, cancelled)
- Payment processing
- Order history

---

## 🐛 Troubleshooting

### Issue: Foreign key constraint violation
**Solution**: Make sure the product exists before creating an order.

### Issue: Product not found when creating order
**Solution**: Use the `/api/orders/create` endpoint with `productId` instead of the product DTO.

### Issue: Timestamp conversion errors
**Solution**: Use ISO 8601 format for dates: `2025-11-10T12:00:00`

---

## ✅ Summary

You now have:
- ✅ Complete Product and Order entities
- ✅ Repositories with custom queries
- ✅ Service layer with business logic
- ✅ REST API controllers
- ✅ Database schema with indexes
- ✅ DTO to Entity mappers

Everything is ready to build and deploy! Just run:
```bash
cd myapp
mvn clean package
cd ..
./scripts/deploy.sh
```
