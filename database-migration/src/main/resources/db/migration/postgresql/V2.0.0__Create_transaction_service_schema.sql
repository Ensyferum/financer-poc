-- V2.0.0 - Transaction Service Database Schema
-- Created: 2025-11-07
-- Author: System
-- Description: Complete transaction management schema with audit, balance tracking and financial rules

-- =====================================================================================
-- TRANSACTION SERVICE SCHEMA
-- =====================================================================================

-- Create transaction service schema
CREATE SCHEMA IF NOT EXISTS transactions;

-- Create transaction types enum
CREATE TYPE transactions.transaction_type AS ENUM (
    'CREDIT',           -- Money coming in
    'DEBIT',            -- Money going out
    'TRANSFER',         -- Between accounts
    'PAYMENT',          -- Payment to external entity
    'DEPOSIT',          -- Cash/check deposit
    'WITHDRAWAL',       -- ATM/cash withdrawal
    'FEE',              -- Bank fees
    'INTEREST',         -- Interest earned/paid
    'REFUND',           -- Transaction reversal
    'ADJUSTMENT'        -- Manual adjustment
);

-- Create transaction status enum
CREATE TYPE transactions.transaction_status AS ENUM (
    'PENDING',          -- Waiting for processing
    'PROCESSING',       -- Being processed
    'COMPLETED',        -- Successfully completed
    'FAILED',           -- Failed processing
    'CANCELLED',        -- Cancelled by user/system
    'REVERSED',         -- Reversed/refunded
    'ON_HOLD',          -- Temporarily held
    'EXPIRED'           -- Expired without processing
);

-- Create currency enum
CREATE TYPE transactions.currency_code AS ENUM (
    'BRL', 'USD', 'EUR', 'GBP', 'JPY', 'CAD', 'AUD', 'CHF', 'CNY', 'SEK'
);

-- =====================================================================================
-- MAIN TABLES
-- =====================================================================================

-- Transactions table
CREATE TABLE transactions.transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_number VARCHAR(50) UNIQUE NOT NULL, -- Unique transaction identifier
    account_id UUID NOT NULL,                       -- Reference to account service
    
    -- Transaction Details
    type transactions.transaction_type NOT NULL,
    status transactions.transaction_status NOT NULL DEFAULT 'PENDING',
    amount DECIMAL(19,4) NOT NULL CHECK (amount > 0),
    currency transactions.currency_code NOT NULL DEFAULT 'BRL',
    
    -- Descriptions
    description TEXT NOT NULL,
    category VARCHAR(100),
    subcategory VARCHAR(100),
    tags TEXT[], -- Array of tags for categorization
    
    -- Reference Information
    reference_id VARCHAR(100),               -- External reference
    batch_id UUID,                          -- For batch processing
    parent_transaction_id UUID,             -- For linked transactions
    original_transaction_id UUID,           -- For reversals/refunds
    
    -- Counterpart Information (for transfers)
    counterpart_account_id UUID,
    counterpart_name VARCHAR(255),
    counterpart_document VARCHAR(50),
    
    -- Geographic and Channel Info
    location_country VARCHAR(3),
    location_city VARCHAR(100),
    channel VARCHAR(50) DEFAULT 'API',      -- API, ATM, ONLINE, MOBILE, BRANCH
    
    -- Processing Information
    processing_date TIMESTAMPTZ,
    value_date DATE,                        -- Date when balance is affected
    authorization_code VARCHAR(100),
    merchant_id VARCHAR(100),
    merchant_category_code VARCHAR(10),
    
    -- Fees and Charges
    fee_amount DECIMAL(19,4) DEFAULT 0,
    tax_amount DECIMAL(19,4) DEFAULT 0,
    exchange_rate DECIMAL(10,6),           -- For currency conversion
    
    -- Metadata
    metadata JSONB,                        -- Flexible additional data
    
    -- Audit fields
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    version INTEGER NOT NULL DEFAULT 1,
    
    -- Constraints
    CONSTRAINT fk_parent_transaction 
        FOREIGN KEY (parent_transaction_id) 
        REFERENCES transactions.transactions(id),
    CONSTRAINT fk_original_transaction 
        FOREIGN KEY (original_transaction_id) 
        REFERENCES transactions.transactions(id),
    CONSTRAINT chk_currency_consistency 
        CHECK (currency IN ('BRL', 'USD', 'EUR', 'GBP', 'JPY', 'CAD', 'AUD', 'CHF', 'CNY', 'SEK'))
);

-- Transaction audit log
CREATE TABLE transactions.transaction_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL,
    
    -- Change tracking
    action VARCHAR(20) NOT NULL,           -- INSERT, UPDATE, DELETE
    old_values JSONB,
    new_values JSONB,
    changed_fields TEXT[],
    
    -- Context
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason TEXT,
    ip_address INET,
    user_agent TEXT,
    
    CONSTRAINT fk_transaction_audit 
        FOREIGN KEY (transaction_id) 
        REFERENCES transactions.transactions(id)
);

-- Balance snapshots for faster calculations
CREATE TABLE transactions.account_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    
    -- Balance information
    available_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    pending_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    blocked_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_balance DECIMAL(19,4) GENERATED ALWAYS AS (available_balance + pending_balance + blocked_balance) STORED,
    
    currency transactions.currency_code NOT NULL DEFAULT 'BRL',
    
    -- Last transaction processed
    last_transaction_id UUID,
    last_processed_at TIMESTAMPTZ,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(account_id, currency),
    
    CONSTRAINT fk_last_transaction 
        FOREIGN KEY (last_transaction_id) 
        REFERENCES transactions.transactions(id)
);

-- Transaction limits and rules
CREATE TABLE transactions.transaction_limits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID,                       -- NULL for global limits
    
    -- Limit configuration
    transaction_type transactions.transaction_type,
    currency transactions.currency_code NOT NULL DEFAULT 'BRL',
    
    -- Limits
    daily_limit DECIMAL(19,4),
    weekly_limit DECIMAL(19,4),
    monthly_limit DECIMAL(19,4),
    single_transaction_limit DECIMAL(19,4),
    
    -- Counters (reset daily)
    daily_used DECIMAL(19,4) DEFAULT 0,
    weekly_used DECIMAL(19,4) DEFAULT 0,
    monthly_used DECIMAL(19,4) DEFAULT 0,
    
    -- Validity
    valid_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_to TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(account_id, transaction_type, currency)
);

-- =====================================================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================================================

-- Primary search indexes
CREATE INDEX idx_transactions_account_id ON transactions.transactions(account_id);
CREATE INDEX idx_transactions_status ON transactions.transactions(status);
CREATE INDEX idx_transactions_type ON transactions.transactions(type);
CREATE INDEX idx_transactions_created_at ON transactions.transactions(created_at DESC);
CREATE INDEX idx_transactions_processing_date ON transactions.transactions(processing_date DESC);
CREATE INDEX idx_transactions_value_date ON transactions.transactions(value_date DESC);

-- Composite indexes for common queries
CREATE INDEX idx_transactions_account_status_date 
    ON transactions.transactions(account_id, status, created_at DESC);
CREATE INDEX idx_transactions_account_type_date 
    ON transactions.transactions(account_id, type, created_at DESC);
CREATE INDEX idx_transactions_status_processing 
    ON transactions.transactions(status, processing_date) 
    WHERE status IN ('PENDING', 'PROCESSING');

-- Reference and relationship indexes
CREATE INDEX idx_transactions_reference_id ON transactions.transactions(reference_id);
CREATE INDEX idx_transactions_batch_id ON transactions.transactions(batch_id);
CREATE INDEX idx_transactions_parent_id ON transactions.transactions(parent_transaction_id);
CREATE INDEX idx_transactions_number ON transactions.transactions(transaction_number);

-- Audit indexes
CREATE INDEX idx_transaction_audit_transaction_id ON transactions.transaction_audit(transaction_id);
CREATE INDEX idx_transaction_audit_changed_at ON transactions.transaction_audit(changed_at DESC);

-- Balance indexes
CREATE INDEX idx_account_balances_account_currency ON transactions.account_balances(account_id, currency);
CREATE INDEX idx_account_balances_updated_at ON transactions.account_balances(updated_at DESC);

-- Limits indexes
CREATE INDEX idx_transaction_limits_account ON transactions.transaction_limits(account_id);
CREATE INDEX idx_transaction_limits_active ON transactions.transaction_limits(is_active, valid_from, valid_to);

-- =====================================================================================
-- FUNCTIONS AND TRIGGERS
-- =====================================================================================

-- Function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION transactions.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for transactions table
CREATE TRIGGER update_transactions_updated_at 
    BEFORE UPDATE ON transactions.transactions 
    FOR EACH ROW EXECUTE FUNCTION transactions.update_updated_at_column();

-- Trigger for account_balances table
CREATE TRIGGER update_account_balances_updated_at 
    BEFORE UPDATE ON transactions.account_balances 
    FOR EACH ROW EXECUTE FUNCTION transactions.update_updated_at_column();

-- Function to generate transaction number
CREATE OR REPLACE FUNCTION transactions.generate_transaction_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.transaction_number IS NULL THEN
        NEW.transaction_number = 'TXN' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || 
                                 LPAD(NEXTVAL('transactions.transaction_number_seq')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Sequence for transaction numbers
CREATE SEQUENCE transactions.transaction_number_seq
    START 1
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 999999
    CYCLE;

-- Trigger to auto-generate transaction numbers
CREATE TRIGGER generate_transaction_number_trigger
    BEFORE INSERT ON transactions.transactions
    FOR EACH ROW EXECUTE FUNCTION transactions.generate_transaction_number();

-- Audit trigger function
CREATE OR REPLACE FUNCTION transactions.transaction_audit_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO transactions.transaction_audit (
            transaction_id, action, new_values, changed_by
        ) VALUES (
            NEW.id, 'INSERT', to_jsonb(NEW), NEW.created_by
        );
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO transactions.transaction_audit (
            transaction_id, action, old_values, new_values, 
            changed_fields, changed_by
        ) VALUES (
            NEW.id, 'UPDATE', to_jsonb(OLD), to_jsonb(NEW),
            ARRAY(SELECT jsonb_object_keys(to_jsonb(NEW) - to_jsonb(OLD))),
            NEW.updated_by
        );
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO transactions.transaction_audit (
            transaction_id, action, old_values, changed_by
        ) VALUES (
            OLD.id, 'DELETE', to_jsonb(OLD), 'SYSTEM'
        );
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ language 'plpgsql';

-- Audit trigger
CREATE TRIGGER transaction_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON transactions.transactions
    FOR EACH ROW EXECUTE FUNCTION transactions.transaction_audit_trigger();

-- =====================================================================================
-- INITIAL DATA
-- =====================================================================================

-- Create default transaction limits
INSERT INTO transactions.transaction_limits (
    account_id, transaction_type, currency, 
    daily_limit, weekly_limit, monthly_limit, single_transaction_limit
) VALUES 
    -- Global default limits for BRL
    (NULL, 'DEBIT', 'BRL', 5000.00, 25000.00, 100000.00, 2000.00),
    (NULL, 'TRANSFER', 'BRL', 10000.00, 50000.00, 200000.00, 5000.00),
    (NULL, 'PAYMENT', 'BRL', 3000.00, 15000.00, 60000.00, 1500.00),
    (NULL, 'WITHDRAWAL', 'BRL', 1000.00, 5000.00, 20000.00, 500.00),
    
    -- Global default limits for USD
    (NULL, 'DEBIT', 'USD', 1000.00, 5000.00, 20000.00, 400.00),
    (NULL, 'TRANSFER', 'USD', 2000.00, 10000.00, 40000.00, 1000.00),
    (NULL, 'PAYMENT', 'USD', 600.00, 3000.00, 12000.00, 300.00),
    (NULL, 'WITHDRAWAL', 'USD', 200.00, 1000.00, 4000.00, 100.00);

-- =====================================================================================
-- VIEWS FOR COMMON QUERIES
-- =====================================================================================

-- View for transaction summary
CREATE VIEW transactions.v_transaction_summary AS
SELECT 
    t.id,
    t.transaction_number,
    t.account_id,
    t.type,
    t.status,
    t.amount,
    t.currency,
    t.description,
    t.category,
    t.processing_date,
    t.value_date,
    t.created_at,
    CASE 
        WHEN t.type IN ('CREDIT', 'DEPOSIT', 'INTEREST', 'REFUND') THEN t.amount
        ELSE 0
    END AS credit_amount,
    CASE 
        WHEN t.type IN ('DEBIT', 'WITHDRAWAL', 'FEE', 'PAYMENT') THEN t.amount
        ELSE 0
    END AS debit_amount
FROM transactions.transactions t;

-- View for account balance calculation
CREATE VIEW transactions.v_account_balance_calculated AS
SELECT 
    account_id,
    currency,
    SUM(CASE WHEN type IN ('CREDIT', 'DEPOSIT', 'INTEREST', 'REFUND') THEN amount ELSE -amount END) as calculated_balance,
    COUNT(*) as transaction_count,
    MAX(created_at) as last_transaction_date
FROM transactions.transactions 
WHERE status = 'COMPLETED'
GROUP BY account_id, currency;

-- =====================================================================================
-- PERMISSIONS AND SECURITY
-- =====================================================================================

-- Create application user for transaction service
-- CREATE USER transaction_service WITH PASSWORD 'txn_service_secure_password_2025';

-- Grant permissions
-- GRANT USAGE ON SCHEMA transactions TO transaction_service;
-- GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA transactions TO transaction_service;
-- GRANT USAGE ON ALL SEQUENCES IN SCHEMA transactions TO transaction_service;

-- Read-only user for reporting
-- CREATE USER transaction_reader WITH PASSWORD 'txn_reader_secure_password_2025';
-- GRANT USAGE ON SCHEMA transactions TO transaction_reader;
-- GRANT SELECT ON ALL TABLES IN SCHEMA transactions TO transaction_reader;

COMMENT ON SCHEMA transactions IS 'Transaction Service Schema - Complete transaction management with audit, balance tracking and financial rules';
COMMENT ON TABLE transactions.transactions IS 'Main transactions table with complete financial transaction data';
COMMENT ON TABLE transactions.transaction_audit IS 'Audit log for all transaction changes';
COMMENT ON TABLE transactions.account_balances IS 'Real-time account balance snapshots for performance';
COMMENT ON TABLE transactions.transaction_limits IS 'Transaction limits and rules per account and type';