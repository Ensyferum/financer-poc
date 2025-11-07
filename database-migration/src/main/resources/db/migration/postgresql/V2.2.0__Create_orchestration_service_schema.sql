-- V2.2.0 - Orchestration Service Database Schema
-- Created: 2025-11-07
-- Author: System
-- Description: Workflow orchestration service with functional programming patterns

-- =====================================================================================
-- ORCHESTRATION SERVICE SCHEMA
-- =====================================================================================

-- Create orchestration service schema
CREATE SCHEMA IF NOT EXISTS orchestration;

-- Create workflow status enum
CREATE TYPE orchestration.workflow_status AS ENUM (
    'CREATED',          -- Workflow definition created
    'ACTIVE',           -- Ready for execution
    'SUSPENDED',        -- Temporarily suspended
    'COMPLETED',        -- Successfully completed
    'FAILED',           -- Failed with errors
    'CANCELLED',        -- Cancelled by user/system
    'ARCHIVED'          -- Archived for historical purposes
);

-- Create execution status enum
CREATE TYPE orchestration.execution_status AS ENUM (
    'PENDING',          -- Waiting to start
    'RUNNING',          -- Currently executing
    'WAITING',          -- Waiting for external input
    'COMPLETED',        -- Successfully completed
    'FAILED',           -- Failed with errors
    'RETRYING',         -- Retrying after failure
    'CANCELLED',        -- Cancelled during execution
    'TIMEOUT'           -- Timed out
);

-- Create step type enum
CREATE TYPE orchestration.step_type AS ENUM (
    'SERVICE_CALL',     -- Call external service
    'DECISION',         -- Conditional branching
    'PARALLEL',         -- Parallel execution
    'LOOP',             -- Iterative processing
    'WAIT',             -- Wait/delay step
    'TRANSFORM',        -- Data transformation
    'VALIDATE',         -- Validation step
    'COMPENSATE',       -- Compensation/rollback
    'HUMAN_TASK',       -- Human intervention required
    'TIMER',            -- Timer-based trigger
    'EVENT',            -- Event-based trigger
    'SCRIPT'            -- Custom script execution
);

-- Create priority enum
CREATE TYPE orchestration.priority_level AS ENUM (
    'CRITICAL',         -- Highest priority
    'HIGH',             -- High priority
    'NORMAL',           -- Normal priority
    'LOW',              -- Low priority
    'BACKGROUND'        -- Background processing
);

-- =====================================================================================
-- MAIN TABLES
-- =====================================================================================

-- Workflow definitions (templates)
CREATE TABLE orchestration.workflow_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Definition metadata
    name VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    tags TEXT[],
    
    -- Workflow configuration
    definition JSONB NOT NULL,              -- Complete workflow definition
    input_schema JSONB,                     -- JSON schema for input validation
    output_schema JSONB,                    -- JSON schema for output validation
    
    -- Functional programming configuration
    is_pure_function BOOLEAN DEFAULT FALSE, -- No side effects
    is_idempotent BOOLEAN DEFAULT TRUE,     -- Can be safely retried
    max_retries INTEGER DEFAULT 3,
    retry_delay_seconds INTEGER DEFAULT 30,
    timeout_seconds INTEGER DEFAULT 3600,
    
    -- Execution settings
    parallel_execution_limit INTEGER DEFAULT 10,
    requires_human_approval BOOLEAN DEFAULT FALSE,
    auto_start BOOLEAN DEFAULT TRUE,
    
    -- Status and lifecycle
    status orchestration.workflow_status NOT NULL DEFAULT 'CREATED',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to TIMESTAMPTZ,
    
    -- Performance tracking
    avg_execution_time_ms INTEGER,
    success_rate DECIMAL(5,2) DEFAULT 100.00,
    total_executions INTEGER DEFAULT 0,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version_number INTEGER NOT NULL DEFAULT 1,
    
    UNIQUE(name, version)
);

-- Workflow executions (instances)
CREATE TABLE orchestration.workflow_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Workflow reference
    workflow_definition_id UUID NOT NULL,
    workflow_name VARCHAR(255) NOT NULL,
    workflow_version VARCHAR(50) NOT NULL,
    
    -- Execution metadata
    execution_name VARCHAR(255),
    correlation_id UUID,                    -- For tracing across services
    parent_execution_id UUID,               -- For sub-workflows
    root_execution_id UUID,                 -- Top-level workflow
    
    -- Input/Output
    input_data JSONB,
    output_data JSONB,
    context_data JSONB,                     -- Mutable context during execution
    
    -- Execution control
    status orchestration.execution_status NOT NULL DEFAULT 'PENDING',
    priority orchestration.priority_level NOT NULL DEFAULT 'NORMAL',
    current_step_id UUID,
    
    -- Timing
    scheduled_start_time TIMESTAMPTZ,
    actual_start_time TIMESTAMPTZ,
    completion_time TIMESTAMPTZ,
    timeout_at TIMESTAMPTZ,
    
    -- Progress tracking
    total_steps INTEGER DEFAULT 0,
    completed_steps INTEGER DEFAULT 0,
    failed_steps INTEGER DEFAULT 0,
    progress_percentage DECIMAL(5,2) DEFAULT 0.00,
    
    -- Error handling
    error_message TEXT,
    error_code VARCHAR(100),
    error_details JSONB,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    
    -- Resource tracking
    execution_time_ms INTEGER,
    cpu_time_ms INTEGER,
    memory_used_mb INTEGER,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT fk_workflow_definition 
        FOREIGN KEY (workflow_definition_id) 
        REFERENCES orchestration.workflow_definitions(id),
    CONSTRAINT fk_parent_execution 
        FOREIGN KEY (parent_execution_id) 
        REFERENCES orchestration.workflow_executions(id),
    CONSTRAINT fk_root_execution 
        FOREIGN KEY (root_execution_id) 
        REFERENCES orchestration.workflow_executions(id)
);

-- Individual step executions
CREATE TABLE orchestration.step_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Workflow context
    workflow_execution_id UUID NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    step_type orchestration.step_type NOT NULL,
    step_definition JSONB NOT NULL,
    
    -- Execution order
    sequence_number INTEGER NOT NULL,
    parent_step_id UUID,                    -- For nested steps
    dependency_step_ids UUID[],             -- Steps that must complete first
    
    -- Input/Output
    input_data JSONB,
    output_data JSONB,
    transformed_data JSONB,
    
    -- Execution details
    status orchestration.execution_status NOT NULL DEFAULT 'PENDING',
    start_time TIMESTAMPTZ,
    completion_time TIMESTAMPTZ,
    timeout_at TIMESTAMPTZ,
    
    -- Service call details (for SERVICE_CALL type)
    service_name VARCHAR(255),
    service_endpoint VARCHAR(500),
    http_method VARCHAR(10),
    request_headers JSONB,
    response_headers JSONB,
    http_status_code INTEGER,
    
    -- Error handling
    error_message TEXT,
    error_code VARCHAR(100),
    error_details JSONB,
    retry_count INTEGER DEFAULT 0,
    is_compensated BOOLEAN DEFAULT FALSE,
    compensation_step_id UUID,
    
    -- Performance
    execution_time_ms INTEGER,
    network_time_ms INTEGER,
    
    -- Functional programming features
    is_pure BOOLEAN DEFAULT FALSE,
    side_effects JSONB,                     -- Documented side effects
    memoization_key VARCHAR(255),           -- For caching results
    cached_result JSONB,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_workflow_execution 
        FOREIGN KEY (workflow_execution_id) 
        REFERENCES orchestration.workflow_executions(id),
    CONSTRAINT fk_parent_step 
        FOREIGN KEY (parent_step_id) 
        REFERENCES orchestration.step_executions(id),
    CONSTRAINT fk_compensation_step 
        FOREIGN KEY (compensation_step_id) 
        REFERENCES orchestration.step_executions(id)
);

-- Event subscriptions and triggers
CREATE TABLE orchestration.workflow_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Event identification
    event_name VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,       -- KAFKA_MESSAGE, HTTP_WEBHOOK, TIMER, MANUAL
    event_source VARCHAR(255),
    
    -- Workflow trigger
    workflow_definition_id UUID,
    trigger_condition JSONB,               -- Conditions to start workflow
    input_mapping JSONB,                   -- How to map event data to workflow input
    
    -- Event configuration
    event_config JSONB,                    -- Event-specific configuration
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Processing
    last_triggered_at TIMESTAMPTZ,
    trigger_count INTEGER DEFAULT 0,
    processing_failures INTEGER DEFAULT 0,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_event_workflow_definition 
        FOREIGN KEY (workflow_definition_id) 
        REFERENCES orchestration.workflow_definitions(id)
);

-- Workflow schedules for time-based execution
CREATE TABLE orchestration.workflow_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Schedule identification
    schedule_name VARCHAR(255) NOT NULL UNIQUE,
    workflow_definition_id UUID NOT NULL,
    
    -- Schedule configuration
    cron_expression VARCHAR(255),           -- Cron-style scheduling
    timezone VARCHAR(50) DEFAULT 'UTC',
    
    -- Execution settings
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    max_concurrent_executions INTEGER DEFAULT 1,
    execution_timeout_seconds INTEGER DEFAULT 3600,
    
    -- Input configuration
    default_input JSONB,
    
    -- Tracking
    last_execution_time TIMESTAMPTZ,
    next_execution_time TIMESTAMPTZ,
    total_executions INTEGER DEFAULT 0,
    successful_executions INTEGER DEFAULT 0,
    failed_executions INTEGER DEFAULT 0,
    
    -- Validity
    effective_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to TIMESTAMPTZ,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_schedule_workflow_definition 
        FOREIGN KEY (workflow_definition_id) 
        REFERENCES orchestration.workflow_definitions(id)
);

-- Saga patterns for distributed transactions
CREATE TABLE orchestration.saga_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Saga identification
    saga_name VARCHAR(255) NOT NULL,
    workflow_execution_id UUID NOT NULL,
    
    -- Transaction details
    transaction_id UUID NOT NULL,
    status orchestration.execution_status NOT NULL DEFAULT 'PENDING',
    
    -- Compensation handling
    compensation_required BOOLEAN DEFAULT FALSE,
    compensation_completed BOOLEAN DEFAULT FALSE,
    compensation_steps JSONB,
    
    -- Participating services
    participating_services TEXT[],
    completed_services TEXT[],
    failed_services TEXT[],
    
    -- Recovery
    rollback_required BOOLEAN DEFAULT FALSE,
    rollback_completed BOOLEAN DEFAULT FALSE,
    rollback_reason TEXT,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_saga_workflow_execution 
        FOREIGN KEY (workflow_execution_id) 
        REFERENCES orchestration.workflow_executions(id)
);

-- Function definitions for reusable workflow components
CREATE TABLE orchestration.function_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Function metadata
    function_name VARCHAR(255) NOT NULL UNIQUE,
    version VARCHAR(50) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    
    -- Functional characteristics
    is_pure BOOLEAN NOT NULL DEFAULT TRUE,
    is_idempotent BOOLEAN NOT NULL DEFAULT TRUE,
    side_effects TEXT[],
    
    -- Function implementation
    implementation_type VARCHAR(50) NOT NULL, -- JAVASCRIPT, GROOVY, KOTLIN, JAVA
    function_code TEXT NOT NULL,
    
    -- Interface
    input_parameters JSONB,
    output_schema JSONB,
    
    -- Performance
    avg_execution_time_ms INTEGER,
    max_execution_time_ms INTEGER DEFAULT 5000,
    memory_limit_mb INTEGER DEFAULT 128,
    
    -- Usage tracking
    usage_count INTEGER DEFAULT 0,
    last_used_at TIMESTAMPTZ,
    
    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    
    UNIQUE(function_name, version)
);

-- =====================================================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================================================

-- Workflow definitions indexes
CREATE INDEX idx_workflow_definitions_name_version ON orchestration.workflow_definitions(name, version);
CREATE INDEX idx_workflow_definitions_status ON orchestration.workflow_definitions(status, is_active);
CREATE INDEX idx_workflow_definitions_category ON orchestration.workflow_definitions(category);

-- Workflow executions indexes
CREATE INDEX idx_workflow_executions_definition ON orchestration.workflow_executions(workflow_definition_id);
CREATE INDEX idx_workflow_executions_status ON orchestration.workflow_executions(status);
CREATE INDEX idx_workflow_executions_correlation ON orchestration.workflow_executions(correlation_id);
CREATE INDEX idx_workflow_executions_parent ON orchestration.workflow_executions(parent_execution_id);
CREATE INDEX idx_workflow_executions_created ON orchestration.workflow_executions(created_at DESC);
CREATE INDEX idx_workflow_executions_priority_status ON orchestration.workflow_executions(priority, status, created_at);

-- Step executions indexes
CREATE INDEX idx_step_executions_workflow ON orchestration.step_executions(workflow_execution_id);
CREATE INDEX idx_step_executions_status ON orchestration.step_executions(status);
CREATE INDEX idx_step_executions_sequence ON orchestration.step_executions(workflow_execution_id, sequence_number);
CREATE INDEX idx_step_executions_type ON orchestration.step_executions(step_type);
CREATE INDEX idx_step_executions_service ON orchestration.step_executions(service_name);
CREATE INDEX idx_step_executions_memoization ON orchestration.step_executions(memoization_key) WHERE memoization_key IS NOT NULL;

-- Events and schedules indexes
CREATE INDEX idx_workflow_events_name_type ON orchestration.workflow_events(event_name, event_type);
CREATE INDEX idx_workflow_events_active ON orchestration.workflow_events(is_active, event_type);
CREATE INDEX idx_workflow_schedules_next_execution ON orchestration.workflow_schedules(next_execution_time) WHERE is_active = true;

-- Functions indexes
CREATE INDEX idx_function_definitions_name_version ON orchestration.function_definitions(function_name, version);
CREATE INDEX idx_function_definitions_category ON orchestration.function_definitions(category, is_active);

-- =====================================================================================
-- FUNCTIONS AND TRIGGERS
-- =====================================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION orchestration.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply triggers to all tables
CREATE TRIGGER update_workflow_definitions_updated_at 
    BEFORE UPDATE ON orchestration.workflow_definitions 
    FOR EACH ROW EXECUTE FUNCTION orchestration.update_updated_at_column();

CREATE TRIGGER update_workflow_executions_updated_at 
    BEFORE UPDATE ON orchestration.workflow_executions 
    FOR EACH ROW EXECUTE FUNCTION orchestration.update_updated_at_column();

CREATE TRIGGER update_step_executions_updated_at 
    BEFORE UPDATE ON orchestration.step_executions 
    FOR EACH ROW EXECUTE FUNCTION orchestration.update_updated_at_column();

CREATE TRIGGER update_workflow_events_updated_at 
    BEFORE UPDATE ON orchestration.workflow_events 
    FOR EACH ROW EXECUTE FUNCTION orchestration.update_updated_at_column();

CREATE TRIGGER update_workflow_schedules_updated_at 
    BEFORE UPDATE ON orchestration.workflow_schedules 
    FOR EACH ROW EXECUTE FUNCTION orchestration.update_updated_at_column();

CREATE TRIGGER update_saga_transactions_updated_at 
    BEFORE UPDATE ON orchestration.saga_transactions 
    FOR EACH ROW EXECUTE FUNCTION orchestration.update_updated_at_column();

CREATE TRIGGER update_function_definitions_updated_at 
    BEFORE UPDATE ON orchestration.function_definitions 
    FOR EACH ROW EXECUTE FUNCTION orchestration.update_updated_at_column();

-- Function to calculate workflow progress
CREATE OR REPLACE FUNCTION orchestration.calculate_workflow_progress()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE orchestration.workflow_executions 
    SET 
        completed_steps = (
            SELECT COUNT(*) 
            FROM orchestration.step_executions 
            WHERE workflow_execution_id = NEW.workflow_execution_id 
              AND status = 'COMPLETED'
        ),
        failed_steps = (
            SELECT COUNT(*) 
            FROM orchestration.step_executions 
            WHERE workflow_execution_id = NEW.workflow_execution_id 
              AND status = 'FAILED'
        ),
        progress_percentage = (
            SELECT CASE 
                WHEN COUNT(*) = 0 THEN 0 
                ELSE (COUNT(*) FILTER (WHERE status = 'COMPLETED')::DECIMAL / COUNT(*) * 100)
            END
            FROM orchestration.step_executions 
            WHERE workflow_execution_id = NEW.workflow_execution_id
        )
    WHERE id = NEW.workflow_execution_id;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for progress calculation
CREATE TRIGGER calculate_workflow_progress_trigger
    AFTER INSERT OR UPDATE OF status ON orchestration.step_executions
    FOR EACH ROW EXECUTE FUNCTION orchestration.calculate_workflow_progress();

-- Function to update workflow statistics
CREATE OR REPLACE FUNCTION orchestration.update_workflow_statistics()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'COMPLETED' OR NEW.status = 'FAILED' THEN
        UPDATE orchestration.workflow_definitions 
        SET 
            total_executions = total_executions + 1,
            success_rate = (
                SELECT (COUNT(*) FILTER (WHERE status = 'COMPLETED')::DECIMAL / COUNT(*) * 100)
                FROM orchestration.workflow_executions 
                WHERE workflow_definition_id = NEW.workflow_definition_id
            ),
            avg_execution_time_ms = (
                SELECT AVG(execution_time_ms)::INTEGER
                FROM orchestration.workflow_executions 
                WHERE workflow_definition_id = NEW.workflow_definition_id 
                  AND execution_time_ms IS NOT NULL
            )
        WHERE id = NEW.workflow_definition_id;
    END IF;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for statistics update
CREATE TRIGGER update_workflow_statistics_trigger
    AFTER UPDATE OF status ON orchestration.workflow_executions
    FOR EACH ROW EXECUTE FUNCTION orchestration.update_workflow_statistics();

-- =====================================================================================
-- VIEWS FOR COMMON QUERIES
-- =====================================================================================

-- Active workflow executions view
CREATE VIEW orchestration.v_active_executions AS
SELECT 
    we.id,
    we.workflow_name,
    we.workflow_version,
    we.execution_name,
    we.status,
    we.priority,
    we.progress_percentage,
    we.created_at,
    we.actual_start_time,
    we.timeout_at,
    wd.description as workflow_description
FROM orchestration.workflow_executions we
JOIN orchestration.workflow_definitions wd ON we.workflow_definition_id = wd.id
WHERE we.status IN ('PENDING', 'RUNNING', 'WAITING', 'RETRYING');

-- Workflow performance metrics view
CREATE VIEW orchestration.v_workflow_metrics AS
SELECT 
    wd.name,
    wd.version,
    wd.total_executions,
    wd.success_rate,
    wd.avg_execution_time_ms,
    COUNT(we.id) as current_month_executions,
    AVG(we.execution_time_ms) as current_month_avg_time,
    COUNT(we.id) FILTER (WHERE we.status = 'COMPLETED') as current_month_success,
    COUNT(we.id) FILTER (WHERE we.status = 'FAILED') as current_month_failures
FROM orchestration.workflow_definitions wd
LEFT JOIN orchestration.workflow_executions we ON wd.id = we.workflow_definition_id 
    AND we.created_at >= DATE_TRUNC('month', CURRENT_DATE)
GROUP BY wd.id, wd.name, wd.version, wd.total_executions, wd.success_rate, wd.avg_execution_time_ms;

-- =====================================================================================
-- STORED PROCEDURES
-- =====================================================================================

-- Function to start workflow execution
CREATE OR REPLACE FUNCTION orchestration.start_workflow_execution(
    p_workflow_name VARCHAR(255),
    p_workflow_version VARCHAR(50) DEFAULT 'latest',
    p_input_data JSONB DEFAULT NULL,
    p_execution_name VARCHAR(255) DEFAULT NULL,
    p_priority orchestration.priority_level DEFAULT 'NORMAL'
)
RETURNS UUID AS $$
DECLARE
    v_workflow_id UUID;
    v_execution_id UUID;
BEGIN
    -- Get workflow definition
    SELECT id INTO v_workflow_id
    FROM orchestration.workflow_definitions
    WHERE name = p_workflow_name 
      AND (version = p_workflow_version OR (p_workflow_version = 'latest' AND is_active = true))
      AND status = 'ACTIVE'
    ORDER BY version_number DESC
    LIMIT 1;
    
    IF v_workflow_id IS NULL THEN
        RAISE EXCEPTION 'Workflow not found: % version %', p_workflow_name, p_workflow_version;
    END IF;
    
    -- Create execution
    INSERT INTO orchestration.workflow_executions (
        workflow_definition_id, workflow_name, workflow_version,
        execution_name, input_data, priority, correlation_id
    ) VALUES (
        v_workflow_id, p_workflow_name, p_workflow_version,
        COALESCE(p_execution_name, p_workflow_name || '_' || EXTRACT(EPOCH FROM NOW())::TEXT),
        p_input_data, p_priority, gen_random_uuid()
    ) RETURNING id INTO v_execution_id;
    
    RETURN v_execution_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================================
-- INITIAL DATA
-- =====================================================================================

-- Sample workflow definitions
INSERT INTO orchestration.workflow_definitions (
    name, version, description, definition, input_schema
) VALUES 
    ('transaction_processing', '1.0', 'Process financial transactions with validation and balance updates',
     '{"steps": [{"name": "validate", "type": "VALIDATE"}, {"name": "process", "type": "SERVICE_CALL"}, {"name": "update_balance", "type": "SERVICE_CALL"}]}'::jsonb,
     '{"type": "object", "properties": {"amount": {"type": "number"}, "accountId": {"type": "string"}}}'::jsonb),
    ('account_creation', '1.0', 'Create new account with verification and setup',
     '{"steps": [{"name": "verify_identity", "type": "SERVICE_CALL"}, {"name": "create_account", "type": "SERVICE_CALL"}, {"name": "setup_defaults", "type": "SERVICE_CALL"}]}'::jsonb,
     '{"type": "object", "properties": {"customerId": {"type": "string"}, "accountType": {"type": "string"}}}'::jsonb);

-- Sample functions
INSERT INTO orchestration.function_definitions (
    function_name, version, description, is_pure, implementation_type, function_code
) VALUES 
    ('calculate_fees', '1.0', 'Calculate transaction fees based on amount and type', true, 'JAVASCRIPT',
     'function calculateFees(amount, transactionType) { return amount * 0.001; }'),
    ('validate_amount', '1.0', 'Validate transaction amount', true, 'JAVASCRIPT',
     'function validateAmount(amount) { return amount > 0 && amount <= 10000; }');

COMMENT ON SCHEMA orchestration IS 'Orchestration Service Schema - Workflow orchestration with functional programming patterns';
COMMENT ON TABLE orchestration.workflow_definitions IS 'Reusable workflow templates and definitions';
COMMENT ON TABLE orchestration.workflow_executions IS 'Active and historical workflow execution instances';
COMMENT ON TABLE orchestration.step_executions IS 'Individual step executions within workflows';
COMMENT ON TABLE orchestration.workflow_events IS 'Event-driven workflow triggers';
COMMENT ON TABLE orchestration.saga_transactions IS 'Saga pattern implementation for distributed transactions';