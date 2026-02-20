-- Add version column for optimistic locking to prevent race conditions in artifacts
ALTER TABLE artifacts ADD COLUMN version BIGINT DEFAULT 0;