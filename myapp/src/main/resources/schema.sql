-- Database Schema for MyApp
-- PostgreSQL 14+

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

-- Grant permissions to application user
-- Run this after creating the myappuser
-- GRANT ALL PRIVILEGES ON TABLE entries TO myappuser;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO myappuser;
