-- Add version column for optimistic locking to prevent race conditions
ALTER TABLE steps ADD COLUMN version BIGINT DEFAULT 0;