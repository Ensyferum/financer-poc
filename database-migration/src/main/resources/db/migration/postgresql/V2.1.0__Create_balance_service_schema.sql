-- V2.1.0 - Balance Service Database Schema
-- Created: 2025-11-07
-- Author: System
-- Description: Real-time balance calculation service with history and projections

-- =====================================================================================
-- BALANCE SERVICE SCHEMA
-- =====================================================================================

-- Create balance service schema
CREATE SCHEMA IF NOT EXISTS balances;

-- Create balance calculation method enum
CREATE TYPE balances.calculation_method AS ENUM (
    'REAL_TIME',        -- Calculated on-demand from transactions
    'SNAPSHOT',         -- Cached from last calculation
    'EVENT_SOURCED',    -- Rebuilt from events
    'HYBRID'            -- Combination of methods
);

-- Create balance type enum
CREATE TYPE balances.balance_type AS ENUM (
    'AVAILABLE',        -- Available for immediate use
    'PENDING',          -- Pending transactions
    'BLOCKED',          -- Blocked/frozen funds
    'RESERVED',         -- Reserved for specific purposes
    'CREDIT_LIMIT',     -- Available credit
    'TOTAL'             -- Total balance (calculated)
);

-- =====================================================================================
-- MAIN TABLES
-- =====================================================================================

-- Real-time balance snapshots
CREATE TABLE balances.account_balance_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    
    -- Balance details
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    balance_type balances.balance_type NOT NULL,
    amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    
    -- Calculation metadata
    calculation_method balances.calculation_method NOT NULL,
    last_transaction_id UUID,
    last_transaction_date TIMESTAMPTZ,
    calculation_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Validity
    valid_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_to TIMESTAMPTZ,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Performance tracking
    calculation_duration_ms INTEGER,
    transaction_count_processed INTEGER DEFAULT 0,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'BALANCE_SERVICE',
    
    UNIQUE(account_id, currency, balance_type, valid_from)
);

-- Balance history for trend analysis
CREATE TABLE balances.balance_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    
    -- Historical data
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    date DATE NOT NULL,
    hour INTEGER CHECK (hour >= 0 AND hour <= 23),
    
    -- Balance amounts at specific time
    opening_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    closing_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    minimum_balance DECIMAL(19,4),
    maximum_balance DECIMAL(19,4),
    average_balance DECIMAL(19,4),
    
    -- Transaction activity
    total_credits DECIMAL(19,4) DEFAULT 0,
    total_debits DECIMAL(19,4) DEFAULT 0,
    transaction_count INTEGER DEFAULT 0,
    credit_count INTEGER DEFAULT 0,
    debit_count INTEGER DEFAULT 0,
    
    -- Metadata
    data_quality_score DECIMAL(3,2) DEFAULT 1.00, -- 0.00 to 1.00
    is_estimated BOOLEAN DEFAULT FALSE,
    notes TEXT,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(account_id, currency, date, hour)
);

-- Balance projections and forecasting
CREATE TABLE balances.balance_projections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    
    -- Projection details
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    projection_date DATE NOT NULL,
    projection_type VARCHAR(50) NOT NULL, -- DAILY, WEEKLY, MONTHLY, YEARLY
    
    -- Projected values
    projected_balance DECIMAL(19,4) NOT NULL,
    confidence_level DECIMAL(3,2) DEFAULT 0.75, -- 0.00 to 1.00
    
    -- Range estimates
    optimistic_balance DECIMAL(19,4),
    pessimistic_balance DECIMAL(19,4),
    most_likely_balance DECIMAL(19,4),
    
    -- Model information
    model_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    model_type VARCHAR(50) NOT NULL DEFAULT 'LINEAR_REGRESSION',
    training_data_period INTEGER, -- Days of historical data used
    
    -- Factors considered
    seasonal_factors JSONB,
    trend_factors JSONB,
    external_factors JSONB,
    
    -- Validation
    actual_balance DECIMAL(19,4), -- Filled when projection date arrives
    accuracy_score DECIMAL(3,2),  -- Calculated accuracy vs actual
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(account_id, currency, projection_date, projection_type)
);

-- Balance alerts and thresholds
CREATE TABLE balances.balance_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    
    -- Alert configuration
    alert_name VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    threshold_type VARCHAR(50) NOT NULL, -- LOW_BALANCE, HIGH_BALANCE, NEGATIVE, UNUSUAL_ACTIVITY
    
    -- Threshold values
    threshold_amount DECIMAL(19,4),
    warning_amount DECIMAL(19,4),
    critical_amount DECIMAL(19,4),
    
    -- Alert behavior
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    alert_frequency VARCHAR(50) DEFAULT 'ONCE', -- ONCE, HOURLY, DAILY
    last_triggered_at TIMESTAMPTZ,
    trigger_count INTEGER DEFAULT 0,
    
    -- Notification settings
    notification_channels TEXT[], -- EMAIL, SMS, PUSH, WEBHOOK
    notification_config JSONB,
    
    -- Validity
    valid_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_to TIMESTAMPTZ,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(account_id, alert_name, currency)
);

-- Balance calculation rules and configurations
CREATE TABLE balances.balance_calculation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Rule identification
    rule_name VARCHAR(255) NOT NULL UNIQUE,
    rule_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    
    -- Rule configuration
    account_type VARCHAR(100), -- NULL for all account types
    currency VARCHAR(3),       -- NULL for all currencies
    
    -- Calculation settings
    calculation_method balances.calculation_method NOT NULL,
    refresh_interval_minutes INTEGER DEFAULT 15,
    include_pending_transactions BOOLEAN DEFAULT TRUE,
    include_blocked_amounts BOOLEAN DEFAULT TRUE,
    
    -- Business rules
    overdraft_allowed BOOLEAN DEFAULT FALSE,
    overdraft_limit DECIMAL(19,4) DEFAULT 0,
    minimum_balance_required DECIMAL(19,4) DEFAULT 0,
    
    -- Performance settings
    cache_duration_minutes INTEGER DEFAULT 5,
    max_concurrent_calculations INTEGER DEFAULT 10,
    enable_real_time_updates BOOLEAN DEFAULT TRUE,
    
    -- Rule logic (JSON configuration)
    calculation_logic JSONB NOT NULL,
    validation_rules JSONB,
    
    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to TIMESTAMPTZ,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);

-- Balance recalculation queue for async processing
CREATE TABLE balances.balance_recalculation_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Target information
    account_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    
    -- Recalculation details
    trigger_reason VARCHAR(100) NOT NULL, -- TRANSACTION_UPDATE, SCHEDULED, MANUAL, ERROR_RECOVERY
    priority INTEGER NOT NULL DEFAULT 5, -- 1-10, where 1 is highest priority
    
    -- Processing information
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    attempts INTEGER DEFAULT 0,
    max_attempts INTEGER DEFAULT 3,
    
    -- Error handling
    last_error_message TEXT,
    last_error_at TIMESTAMPTZ,
    
    -- Performance tracking
    processing_started_at TIMESTAMPTZ,
    processing_completed_at TIMESTAMPTZ,
    processing_duration_ms INTEGER,
    
    -- Request metadata
    requested_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    correlation_id UUID,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================================================

-- Account balance snapshots indexes
CREATE INDEX idx_balance_snapshots_account_id ON balances.account_balance_snapshots(account_id);
CREATE INDEX idx_balance_snapshots_currency ON balances.account_balance_snapshots(currency);
CREATE INDEX idx_balance_snapshots_current ON balances.account_balance_snapshots(account_id, currency, balance_type) 
    WHERE is_current = true;
CREATE INDEX idx_balance_snapshots_calculation_timestamp ON balances.account_balance_snapshots(calculation_timestamp DESC);

-- Balance history indexes
CREATE INDEX idx_balance_history_account_date ON balances.balance_history(account_id, currency, date DESC);
CREATE INDEX idx_balance_history_date ON balances.balance_history(date DESC, hour);
CREATE INDEX idx_balance_history_account_currency ON balances.balance_history(account_id, currency);

-- Balance projections indexes
CREATE INDEX idx_balance_projections_account_date ON balances.balance_projections(account_id, projection_date);
CREATE INDEX idx_balance_projections_date ON balances.balance_projections(projection_date DESC);
CREATE INDEX idx_balance_projections_accuracy ON balances.balance_projections(accuracy_score DESC) 
    WHERE accuracy_score IS NOT NULL;

-- Balance alerts indexes
CREATE INDEX idx_balance_alerts_account_active ON balances.balance_alerts(account_id) 
    WHERE is_active = true;
CREATE INDEX idx_balance_alerts_triggered ON balances.balance_alerts(last_triggered_at DESC) 
    WHERE last_triggered_at IS NOT NULL;

-- Balance calculation rules indexes
CREATE INDEX idx_balance_calc_rules_active ON balances.balance_calculation_rules(is_active, effective_from, effective_to);
CREATE INDEX idx_balance_calc_rules_account_type ON balances.balance_calculation_rules(account_type, currency);

-- Recalculation queue indexes
CREATE INDEX idx_balance_recalc_queue_status_priority ON balances.balance_recalculation_queue(status, priority, created_at);
CREATE INDEX idx_balance_recalc_queue_account ON balances.balance_recalculation_queue(account_id, status);

-- =====================================================================================
-- FUNCTIONS AND TRIGGERS
-- =====================================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION balances.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to all tables
CREATE TRIGGER update_balance_snapshots_updated_at 
    BEFORE UPDATE ON balances.account_balance_snapshots 
    FOR EACH ROW EXECUTE FUNCTION balances.update_updated_at_column();

CREATE TRIGGER update_balance_history_updated_at 
    BEFORE UPDATE ON balances.balance_history 
    FOR EACH ROW EXECUTE FUNCTION balances.update_updated_at_column();

CREATE TRIGGER update_balance_projections_updated_at 
    BEFORE UPDATE ON balances.balance_projections 
    FOR EACH ROW EXECUTE FUNCTION balances.update_updated_at_column();

CREATE TRIGGER update_balance_alerts_updated_at 
    BEFORE UPDATE ON balances.balance_alerts 
    FOR EACH ROW EXECUTE FUNCTION balances.update_updated_at_column();

CREATE TRIGGER update_balance_calc_rules_updated_at 
    BEFORE UPDATE ON balances.balance_calculation_rules 
    FOR EACH ROW EXECUTE FUNCTION balances.update_updated_at_column();

CREATE TRIGGER update_balance_recalc_queue_updated_at 
    BEFORE UPDATE ON balances.balance_recalculation_queue 
    FOR EACH ROW EXECUTE FUNCTION balances.update_updated_at_column();

-- Function to invalidate old snapshots when creating new ones
CREATE OR REPLACE FUNCTION balances.invalidate_old_snapshots()
RETURNS TRIGGER AS $$
BEGIN
    -- Mark old snapshots as not current
    UPDATE balances.account_balance_snapshots 
    SET is_current = false, valid_to = CURRENT_TIMESTAMP
    WHERE account_id = NEW.account_id 
      AND currency = NEW.currency 
      AND balance_type = NEW.balance_type 
      AND is_current = true 
      AND id != NEW.id;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for snapshot invalidation
CREATE TRIGGER invalidate_old_snapshots_trigger
    AFTER INSERT ON balances.account_balance_snapshots
    FOR EACH ROW EXECUTE FUNCTION balances.invalidate_old_snapshots();

-- Function to calculate balance accuracy
CREATE OR REPLACE FUNCTION balances.calculate_projection_accuracy()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.actual_balance IS NOT NULL AND OLD.actual_balance IS NULL THEN
        NEW.accuracy_score = GREATEST(0, 1 - ABS(NEW.projected_balance - NEW.actual_balance) / GREATEST(NEW.projected_balance, NEW.actual_balance, 1));
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for accuracy calculation
CREATE TRIGGER calculate_projection_accuracy_trigger
    BEFORE UPDATE ON balances.balance_projections
    FOR EACH ROW EXECUTE FUNCTION balances.calculate_projection_accuracy();

-- =====================================================================================
-- VIEWS FOR COMMON QUERIES
-- =====================================================================================

-- Current balance view (most frequently used)
CREATE VIEW balances.v_current_balances AS
SELECT 
    s.account_id,
    s.currency,
    s.balance_type,
    s.amount,
    s.calculation_timestamp,
    s.last_transaction_date,
    s.calculation_method
FROM balances.account_balance_snapshots s
WHERE s.is_current = true;

-- Account summary view
CREATE VIEW balances.v_account_balance_summary AS
SELECT 
    account_id,
    currency,
    SUM(CASE WHEN balance_type = 'AVAILABLE' THEN amount ELSE 0 END) as available_balance,
    SUM(CASE WHEN balance_type = 'PENDING' THEN amount ELSE 0 END) as pending_balance,
    SUM(CASE WHEN balance_type = 'BLOCKED' THEN amount ELSE 0 END) as blocked_balance,
    SUM(CASE WHEN balance_type = 'RESERVED' THEN amount ELSE 0 END) as reserved_balance,
    SUM(amount) as total_balance,
    MAX(calculation_timestamp) as last_updated
FROM balances.v_current_balances
GROUP BY account_id, currency;

-- Daily balance trend view
CREATE VIEW balances.v_daily_balance_trends AS
SELECT 
    account_id,
    currency,
    date,
    closing_balance,
    (closing_balance - LAG(closing_balance) OVER (PARTITION BY account_id, currency ORDER BY date)) as daily_change,
    ((closing_balance - LAG(closing_balance) OVER (PARTITION BY account_id, currency ORDER BY date)) / 
     NULLIF(LAG(closing_balance) OVER (PARTITION BY account_id, currency ORDER BY date), 0) * 100) as daily_change_percentage,
    total_credits,
    total_debits,
    transaction_count
FROM balances.balance_history
WHERE hour IS NULL; -- Daily aggregates only

-- =====================================================================================
-- INITIAL DATA AND CONFIGURATION
-- =====================================================================================

-- Default balance calculation rules
INSERT INTO balances.balance_calculation_rules (
    rule_name, rule_version, calculation_method, 
    refresh_interval_minutes, calculation_logic
) VALUES 
    ('DEFAULT_REAL_TIME', '1.0', 'REAL_TIME', 1, 
     '{"include_pending": true, "include_blocked": true, "cache_seconds": 30}'::jsonb),
    ('DEFAULT_SNAPSHOT', '1.0', 'SNAPSHOT', 15, 
     '{"rebuild_threshold_transactions": 100, "max_age_hours": 1}'::jsonb),
    ('HIGH_FREQUENCY_ACCOUNTS', '1.0', 'HYBRID', 5, 
     '{"real_time_threshold": 1000, "snapshot_fallback": true}'::jsonb);

-- =====================================================================================
-- STORED PROCEDURES FOR BALANCE OPERATIONS
-- =====================================================================================

-- Function to get current balance for an account
CREATE OR REPLACE FUNCTION balances.get_current_balance(
    p_account_id UUID,
    p_currency VARCHAR(3) DEFAULT 'BRL',
    p_balance_type balances.balance_type DEFAULT 'AVAILABLE'
)
RETURNS DECIMAL(19,4) AS $$
DECLARE
    v_balance DECIMAL(19,4);
BEGIN
    SELECT amount INTO v_balance
    FROM balances.v_current_balances
    WHERE account_id = p_account_id
      AND currency = p_currency
      AND balance_type = p_balance_type;
    
    RETURN COALESCE(v_balance, 0);
END;
$$ LANGUAGE plpgsql;

-- Function to queue balance recalculation
CREATE OR REPLACE FUNCTION balances.queue_balance_recalculation(
    p_account_id UUID,
    p_currency VARCHAR(3) DEFAULT 'BRL',
    p_reason VARCHAR(100) DEFAULT 'MANUAL',
    p_priority INTEGER DEFAULT 5
)
RETURNS UUID AS $$
DECLARE
    v_queue_id UUID;
BEGIN
    INSERT INTO balances.balance_recalculation_queue (
        account_id, currency, trigger_reason, priority
    ) VALUES (
        p_account_id, p_currency, p_reason, p_priority
    ) RETURNING id INTO v_queue_id;
    
    RETURN v_queue_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================================
-- PERMISSIONS AND SECURITY
-- =====================================================================================

-- Create application user for balance service
-- CREATE USER balance_service WITH PASSWORD 'balance_service_secure_password_2025';

-- Grant permissions
-- GRANT USAGE ON SCHEMA balances TO balance_service;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA balances TO balance_service;
-- GRANT USAGE ON ALL SEQUENCES IN SCHEMA balances TO balance_service;
-- GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA balances TO balance_service;

-- Read-only user for reporting
-- CREATE USER balance_reader WITH PASSWORD 'balance_reader_secure_password_2025';
-- GRANT USAGE ON SCHEMA balances TO balance_reader;
-- GRANT SELECT ON ALL TABLES IN SCHEMA balances TO balance_reader;
-- GRANT EXECUTE ON FUNCTION balances.get_current_balance TO balance_reader;

COMMENT ON SCHEMA balances IS 'Balance Service Schema - Real-time balance calculation with history and projections';
COMMENT ON TABLE balances.account_balance_snapshots IS 'Current balance snapshots for real-time access';
COMMENT ON TABLE balances.balance_history IS 'Historical balance data for trend analysis';
COMMENT ON TABLE balances.balance_projections IS 'Balance forecasting and projections';
COMMENT ON TABLE balances.balance_alerts IS 'Balance-based alerts and notifications';
COMMENT ON TABLE balances.balance_calculation_rules IS 'Configuration for balance calculation methods';
COMMENT ON TABLE balances.balance_recalculation_queue IS 'Async balance recalculation queue';