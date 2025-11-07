-- ================================================================
-- Migration: V1.0.0__Create_initial_database_structure.sql
-- Description: Creates the initial database structure for Financer
-- Author: Financer Development Team (Python Migration)
-- Date: 2025-11-06
-- ================================================================

-- Create user if not exists (will be handled by baseline)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'financer_user') THEN
        CREATE USER financer_user WITH PASSWORD 'financer123';
    END IF;
END
$$;

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON DATABASE financer_accounts TO financer_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO financer_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO financer_user;
GRANT CREATE ON SCHEMA public TO financer_user;
GRANT USAGE ON SCHEMA public TO financer_user;

-- Grant future privileges
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO financer_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO financer_user;

-- Create accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number VARCHAR(20) NOT NULL UNIQUE,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('CHECKING','SAVINGS','INVESTMENT','BUSINESS')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED','CLOSED','PENDING_VERIFICATION')),
    balance NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    daily_limit NUMERIC(15,2),
    monthly_limit NUMERIC(15,2),
    owner_name VARCHAR(255) NOT NULL,
    owner_document VARCHAR(20) NOT NULL,
    owner_email VARCHAR(100),
    bank_code VARCHAR(10),
    branch_code VARCHAR(10),
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

-- Create account audit table for change tracking
CREATE TABLE IF NOT EXISTS account_audit (
    id BIGSERIAL PRIMARY KEY,
    account_id UUID NOT NULL,
    operation VARCHAR(10) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by VARCHAR(255),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_accounts_account_number ON accounts (account_number);
CREATE INDEX IF NOT EXISTS idx_accounts_owner_document ON accounts (owner_document);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts (status);
CREATE INDEX IF NOT EXISTS idx_accounts_account_type ON accounts (account_type);
CREATE INDEX IF NOT EXISTS idx_accounts_created_at ON accounts (created_at);
CREATE INDEX IF NOT EXISTS idx_accounts_active ON accounts (active) WHERE active = true;

-- Create indexes for audit table
CREATE INDEX IF NOT EXISTS idx_account_audit_account_id ON account_audit (account_id);
CREATE INDEX IF NOT EXISTS idx_account_audit_changed_at ON account_audit (changed_at);
CREATE INDEX IF NOT EXISTS idx_account_audit_operation ON account_audit (operation);

-- Create audit trigger function
CREATE OR REPLACE FUNCTION account_audit_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO account_audit (account_id, operation, old_values, changed_by)
        VALUES (OLD.id, TG_OP, row_to_json(OLD), current_user);
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO account_audit (account_id, operation, old_values, new_values, changed_by)
        VALUES (NEW.id, TG_OP, row_to_json(OLD), row_to_json(NEW), current_user);
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO account_audit (account_id, operation, new_values, changed_by)
        VALUES (NEW.id, TG_OP, row_to_json(NEW), current_user);
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create audit trigger
DROP TRIGGER IF EXISTS account_audit_trigger ON accounts;
CREATE TRIGGER account_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON accounts
    FOR EACH ROW EXECUTE FUNCTION account_audit_trigger_function();

-- Insert sample data for testing
INSERT INTO accounts (account_number, account_type, status, balance, owner_name, owner_document, owner_email, created_by)
VALUES 
    ('ACC-001', 'CHECKING', 'ACTIVE', 1000.00, 'John Doe', '123.456.789-00', 'john.doe@email.com', 'python-migration'),
    ('ACC-002', 'SAVINGS', 'ACTIVE', 5000.00, 'Jane Smith', '987.654.321-00', 'jane.smith@email.com', 'python-migration')
ON CONFLICT (account_number) DO NOTHING;

-- Create sequences for account numbering
CREATE SEQUENCE IF NOT EXISTS account_number_seq START WITH 1000;

-- Add comments for documentation
COMMENT ON TABLE accounts IS 'Main accounts table storing all account information';
COMMENT ON TABLE account_audit IS 'Audit trail for all changes to accounts table';
COMMENT ON COLUMN accounts.id IS 'Primary key - UUID for account identification';
COMMENT ON COLUMN accounts.account_number IS 'Unique account number for external references';
COMMENT ON COLUMN accounts.version IS 'Optimistic locking version field';