-- Add description column to tasks table
-- This column was missing from the initial schema but is required by the Task entity

ALTER TABLE tasks ADD COLUMN description TEXT;