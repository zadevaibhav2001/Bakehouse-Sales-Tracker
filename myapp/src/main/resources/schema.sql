-- Database Schema for MyApp
-- PostgreSQL 14+

-- ============================================
-- SYNC TABLES (Mobile Sync API)
-- ============================================

-- Create entries table
CREATE TABLE IF NOT EXISTS entries (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    payload JSONB,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL
);

-- Create index for efficient queries
CREATE INDEX IF NOT EXISTS idx_entries_user_updated 
ON entries (user_id, updated_at);

-- Optional: Create index on deleted flag for filtering
CREATE INDEX IF NOT EXISTS idx_entries_deleted 
ON entries (deleted) WHERE deleted = false;

-- ============================================
-- PRODUCT AND ORDER TABLES
-- ============================================

-- Create products table
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    in_stock BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
    order_id UUID PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    order_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Create indexes for products
CREATE INDEX IF NOT EXISTS idx_products_in_stock ON products (in_stock);
CREATE INDEX IF NOT EXISTS idx_products_name ON products (name);

-- Create indexes for orders
CREATE INDEX IF NOT EXISTS idx_orders_order_date ON orders (order_date_time);
CREATE INDEX IF NOT EXISTS idx_orders_product ON orders (product_id);

-- ============================================
-- GRANT PERMISSIONS
-- ============================================
-- Run this after creating the myappuser
-- GRANT ALL PRIVILEGES ON TABLE entries TO myappuser;
-- GRANT ALL PRIVILEGES ON TABLE products TO myappuser;
-- GRANT ALL PRIVILEGES ON TABLE orders TO myappuser;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO myappuser;
