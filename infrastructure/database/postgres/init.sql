-- Inicialização do PostgreSQL para o Sistema Financer
-- Este script cria os schemas e usuários necessários

-- Criar schema para contas
CREATE SCHEMA IF NOT EXISTS account_schema;

-- Criar schema para transações
CREATE SCHEMA IF NOT EXISTS transaction_schema;

-- Criar schema para auditoria
CREATE SCHEMA IF NOT EXISTS audit_schema;

-- Criar schema para orquestração (CAMUNDA)
CREATE SCHEMA IF NOT EXISTS orchestration_schema;

-- Criar usuário para o serviço de contas
CREATE USER account_service WITH PASSWORD 'account123';
GRANT USAGE ON SCHEMA account_schema TO account_service;
GRANT ALL PRIVILEGES ON SCHEMA account_schema TO account_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA account_schema TO account_service;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA account_schema TO account_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA account_schema GRANT ALL ON TABLES TO account_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA account_schema GRANT ALL ON SEQUENCES TO account_service;

-- Criar usuário para o serviço de transações
CREATE USER transaction_service WITH PASSWORD 'transaction123';
GRANT USAGE ON SCHEMA transaction_schema TO transaction_service;
GRANT ALL PRIVILEGES ON SCHEMA transaction_schema TO transaction_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA transaction_schema TO transaction_service;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA transaction_schema TO transaction_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA transaction_schema GRANT ALL ON TABLES TO transaction_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA transaction_schema GRANT ALL ON SEQUENCES TO transaction_service;

-- Criar usuário para o serviço de orquestração (CAMUNDA)
CREATE USER orchestration_service WITH PASSWORD 'orchestration123';
GRANT USAGE ON SCHEMA orchestration_schema TO orchestration_service;
GRANT ALL PRIVILEGES ON SCHEMA orchestration_schema TO orchestration_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA orchestration_schema TO orchestration_service;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA orchestration_schema TO orchestration_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA orchestration_schema GRANT ALL ON TABLES TO orchestration_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA orchestration_schema GRANT ALL ON SEQUENCES TO orchestration_service;

-- Usuário para auditoria (usado por todos os serviços)
CREATE USER audit_service WITH PASSWORD 'audit123';
GRANT USAGE ON SCHEMA audit_schema TO audit_service;
GRANT ALL PRIVILEGES ON SCHEMA audit_schema TO audit_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA audit_schema TO audit_service;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA audit_schema TO audit_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit_schema GRANT ALL ON TABLES TO audit_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit_schema GRANT ALL ON SEQUENCES TO audit_service;

-- Conceder permissões de auditoria para outros usuários
GRANT USAGE ON SCHEMA audit_schema TO account_service;
GRANT INSERT, SELECT ON ALL TABLES IN SCHEMA audit_schema TO account_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit_schema GRANT INSERT, SELECT ON TABLES TO account_service;

GRANT USAGE ON SCHEMA audit_schema TO transaction_service;
GRANT INSERT, SELECT ON ALL TABLES IN SCHEMA audit_schema TO transaction_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit_schema GRANT INSERT, SELECT ON TABLES TO transaction_service;

-- Conceder permissões de auditoria para serviço de orquestração
GRANT USAGE ON SCHEMA audit_schema TO orchestration_service;
GRANT INSERT, SELECT ON ALL TABLES IN SCHEMA audit_schema TO orchestration_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit_schema GRANT INSERT, SELECT ON TABLES TO orchestration_service;

-- Extensões úteis
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

COMMENT ON DATABASE financer IS 'Base de dados do Sistema de Gestão Financeira';
COMMENT ON SCHEMA account_schema IS 'Schema para gestão de contas bancárias e cartões';
COMMENT ON SCHEMA transaction_schema IS 'Schema para gestão de transações financeiras';
COMMENT ON SCHEMA audit_schema IS 'Schema para auditoria e controle de alterações';