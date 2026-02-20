-- Initial database schema for Agentic Backend System
-- Creates tables: tasks, runs, steps, artifacts

-- Table: tasks
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for tasks table
CREATE INDEX idx_tasks_id ON tasks(id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_created_at ON tasks(created_at);

-- Table: runs
CREATE TABLE runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    claimed_by VARCHAR(255),
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Foreign key constraint (optional - depends on if you want referential integrity)
    CONSTRAINT fk_runs_task_id FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- Indexes for runs table
CREATE INDEX idx_runs_status_created ON runs(status, created_at);
CREATE INDEX idx_runs_task_id ON runs(task_id);
CREATE INDEX idx_runs_claimed_by ON runs(claimed_by);
CREATE INDEX idx_runs_id ON runs(id);

-- Table: steps
CREATE TABLE steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    run_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_steps_run_id FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE CASCADE
);

-- Indexes for steps table
CREATE INDEX idx_steps_run_id ON steps(run_id);
CREATE INDEX idx_steps_status ON steps(status);
CREATE INDEX idx_steps_id ON steps(id);
CREATE INDEX idx_steps_created_at ON steps(created_at);

-- Table: artifacts
CREATE TABLE artifacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    mime_type VARCHAR(100),
    content TEXT,
    file_path VARCHAR(500),
    size BIGINT,
    step_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_artifacts_step_id FOREIGN KEY (step_id) REFERENCES steps(id) ON DELETE CASCADE
);

-- Indexes for artifacts table
CREATE INDEX idx_artifacts_step_id ON artifacts(step_id);
CREATE INDEX idx_artifacts_id ON artifacts(id);
CREATE INDEX idx_artifacts_type ON artifacts(type);
CREATE INDEX idx_artifacts_mime_type ON artifacts(mime_type);
CREATE INDEX idx_artifacts_created_at ON artifacts(created_at);

-- Create enum-like constraints for status fields
ALTER TABLE runs ADD CONSTRAINT chk_runs_status 
    CHECK (status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED', 'CANCELLED'));

ALTER TABLE steps ADD CONSTRAINT chk_steps_status 
    CHECK (status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED', 'SKIPPED'));

-- Add comments for documentation
COMMENT ON TABLE tasks IS 'Main task entities that represent user work requests';
COMMENT ON TABLE runs IS 'Execution runs for tasks with atomic claiming support';
COMMENT ON TABLE steps IS 'Individual execution steps within a run (Planning, Execution, Validation)';
COMMENT ON TABLE artifacts IS 'Generated artifacts from step execution (files, logs, results)';

COMMENT ON COLUMN runs.claimed_by IS 'Instance ID that claimed this run for processing';
COMMENT ON COLUMN runs.started_at IS 'When the run execution actually started';
COMMENT ON COLUMN runs.finished_at IS 'When the run execution completed (success or failure)';
COMMENT ON COLUMN artifacts.size IS 'Size of the artifact content in bytes';
COMMENT ON COLUMN artifacts.file_path IS 'Optional file system path for stored artifacts';
COMMENT ON COLUMN artifacts.mime_type IS 'MIME type of the artifact content for proper handling';