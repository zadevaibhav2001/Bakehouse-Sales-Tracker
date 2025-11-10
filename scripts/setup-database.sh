#!/bin/bash
# ============================================
# Database Setup Script
# ============================================
# Run this script on the EC2 instance after bootstrap
# to initialize the database schema

set -e

DB_NAME="myappdb"
DB_USER="myappuser"
DB_PASSWORD="${DB_PASSWORD:-<DB_PASSWORD>}"  # Use env var or replace placeholder

echo "Setting up database schema..."

# Run schema creation
PGPASSWORD="$DB_PASSWORD" psql -U "$DB_USER" -d "$DB_NAME" -h localhost <<EOF
-- Create entries table
CREATE TABLE IF NOT EXISTS entries (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    payload JSONB,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_entries_user_updated 
ON entries (user_id, updated_at);

CREATE INDEX IF NOT EXISTS idx_entries_deleted 
ON entries (deleted) WHERE deleted = false;

-- Verify tables
\dt

-- Show indexes
\di
EOF

echo "Database schema setup completed!"
echo ""
echo "To verify, run:"
echo "  PGPASSWORD='$DB_PASSWORD' psql -U $DB_USER -d $DB_NAME -c '\dt'"
