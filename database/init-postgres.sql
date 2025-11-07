-- Database initialization script for Financer PostgreSQL
-- Creates the account database and user

-- Create the account database
CREATE DATABASE financer_accounts;

-- Connect to the account database
\c financer_accounts;

-- Create user and grant permissions
CREATE USER financer_user WITH PASSWORD 'financer123';
GRANT ALL PRIVILEGES ON DATABASE financer_accounts TO financer_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO financer_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO financer_user;

-- Grant future privileges
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO financer_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO financer_user;