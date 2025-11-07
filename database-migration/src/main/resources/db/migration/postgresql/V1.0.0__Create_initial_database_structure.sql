-- ================================================================
-- Migration: V1.0.0__Create_initial_database_structure.sql
-- Description: Creates the initial database structure for Financer
-- Author: Financer Development Team
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

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_account_status ON accounts (status);
CREATE INDEX IF NOT EXISTS idx_account_type ON accounts (account_type);
CREATE INDEX IF NOT EXISTS idx_owner_name ON accounts (owner_name);
CREATE INDEX IF NOT EXISTS idx_owner_document ON accounts (owner_document);
CREATE INDEX IF NOT EXISTS idx_account_number ON accounts (account_number);

-- Create audit table for account changes
CREATE TABLE IF NOT EXISTS account_audit (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    operation VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    old_values JSONB,
    new_values JSONB,
    changed_by VARCHAR(255),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

-- Create index for audit table
CREATE INDEX IF NOT EXISTS idx_account_audit_account_id ON account_audit (account_id);
CREATE INDEX IF NOT EXISTS idx_account_audit_changed_at ON account_audit (changed_at);

-- Create trigger function for audit
CREATE OR REPLACE FUNCTION audit_account_changes() 
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO account_audit (account_id, operation, old_values, changed_by)
        VALUES (OLD.id, 'DELETE', row_to_json(OLD), OLD.updated_by);
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO account_audit (account_id, operation, old_values, new_values, changed_by)
        VALUES (NEW.id, 'UPDATE', row_to_json(OLD), row_to_json(NEW), NEW.updated_by);
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO account_audit (account_id, operation, new_values, changed_by)
        VALUES (NEW.id, 'INSERT', row_to_json(NEW), NEW.created_by);
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create trigger
DROP TRIGGER IF EXISTS account_audit_trigger ON accounts;
CREATE TRIGGER account_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON accounts
    FOR EACH ROW EXECUTE FUNCTION audit_account_changes();

-- Insert default data for development
INSERT INTO accounts (
    account_number, account_type, status, balance, owner_name, owner_document, 
    owner_email, bank_code, branch_code, description, created_by
) VALUES 
(
    '1001-0001', 'CHECKING', 'ACTIVE', 1000.00, 'Admin User', '000.000.000-01',
    'admin@financer.com', '001', '0001', 'Admin checking account', 'SYSTEM'
) ON CONFLICT (account_number) DO NOTHING;

-- Grant permissions on new tables
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO financer_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO financer_user;