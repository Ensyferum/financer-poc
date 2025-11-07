"""
Financer Database Migration System - Configuration Manager
=========================================================

Serverless Python migration system with Config Server integration,
maintaining all enterprise features from the original Spring Boot version.
"""

import os
import requests
import logging
from typing import Dict, Any, Optional
from dataclasses import dataclass
from requests.auth import HTTPBasicAuth
import json
from dotenv import load_dotenv


@dataclass
class DatabaseConfig:
    """Database connection configuration"""
    url: str
    username: str
    password: str
    schema: str = "public"
    
    
@dataclass 
class MongoConfig:
    """MongoDB connection configuration"""
    url: str
    database: str
    

@dataclass
class ControlConfig:
    """Migration control configuration"""
    enabled: bool = True
    table_name: str = "migration_execution_history"
    

@dataclass
class ExecutionConfig:
    """Migration execution configuration"""
    generate_report: bool = True
    report_format: str = "JSON"
    max_retries: int = 3
    timeout_minutes: int = 30


@dataclass
class MigrationConfig:
    """Complete migration configuration"""
    postgres: DatabaseConfig
    mongodb: MongoConfig
    control: ControlConfig
    execution: ExecutionConfig
    

class ConfigManager:
    """
    Configuration manager that loads settings from Config Server
    with fallback to environment variables and .env file
    """
    
    def __init__(self, profile: str = "local"):
        load_dotenv()
        self.profile = profile
        self.config_server_url = os.getenv("CONFIG_SERVER_URL", "http://localhost:8888")
        self.config_server_username = os.getenv("CONFIG_SERVER_USERNAME", "config-admin")
        self.config_server_password = os.getenv("CONFIG_SERVER_PASSWORD", "config123")
        self.config_server_timeout = int(os.getenv("CONFIG_SERVER_TIMEOUT", "30"))
        self.application_name = os.getenv("APPLICATION_NAME", "database-migration")
        
        self._config_cache: Optional[Dict[str, Any]] = None
        self._logger = logging.getLogger(__name__)
        
    def load_config(self) -> MigrationConfig:
        """
        Load configuration from Config Server with fallback to environment variables
        """
        # Temporary: Force environment configuration for MongoDB fix
        self._logger.info("📁 Using environment configuration (forced)")
        return self._load_from_environment()
        
        # try:
        #     # Try to load from Config Server first
        #     config_data = self._load_from_config_server()
        #     if config_data:
        #         self._logger.info("✅ Configuration loaded from Config Server")
        #         return self._parse_config(config_data)
        # except Exception as e:
        #     self._logger.warning(f"⚠️ Failed to load from Config Server: {e}")
            
        # # Fallback to environment variables
        # self._logger.info("📁 Using fallback configuration from environment")
        # return self._load_from_environment()
    
    def _load_from_config_server(self) -> Optional[Dict[str, Any]]:
        """Load configuration from Spring Cloud Config Server"""
        url = f"{self.config_server_url}/{self.application_name}/{self.profile}"
        
        try:
            auth = HTTPBasicAuth(self.config_server_username, self.config_server_password)
            response = requests.get(url, auth=auth, timeout=self.config_server_timeout)
            response.raise_for_status()
            
            config_response = response.json()
            
            # Extract properties from Spring Cloud Config response format
            properties = {}
            for source in config_response.get("propertySources", []):
                source_properties = source.get("source", {})
                properties.update(source_properties)
                
            return properties
            
        except requests.exceptions.RequestException as e:
            self._logger.error(f"Config Server request failed: {e}")
            return None
        except json.JSONDecodeError as e:
            self._logger.error(f"Failed to parse Config Server response: {e}")
            return None
    
    def _load_from_environment(self) -> MigrationConfig:
        """Load configuration from environment variables"""
        
        # PostgreSQL configuration
        postgres_config = DatabaseConfig(
            url=os.getenv("POSTGRES_URL", "postgresql://financer:financer123@localhost:5432/financer"),
            username=os.getenv("POSTGRES_USERNAME", "financer"),
            password=os.getenv("POSTGRES_PASSWORD", "financer123"),
            schema=os.getenv("POSTGRES_SCHEMA", "public")
        )
        
        # MongoDB configuration  
        mongodb_config = MongoConfig(
            url=os.getenv("MONGODB_URL", "mongodb://financer:financer123@localhost:27017/financer?authSource=admin"),
            database=os.getenv("MONGODB_DATABASE", "financer")
        )
        
        # Control configuration
        control_config = ControlConfig(
            enabled=os.getenv("MIGRATION_CONTROL_ENABLED", "true").lower() == "true",
            table_name=os.getenv("MIGRATION_CONTROL_TABLE", "migration_execution_history")
        )
        
        # Execution configuration
        execution_config = ExecutionConfig(
            generate_report=os.getenv("MIGRATION_GENERATE_REPORT", "true").lower() == "true",
            report_format=os.getenv("MIGRATION_REPORT_FORMAT", "JSON"),
            max_retries=int(os.getenv("MIGRATION_MAX_RETRIES", "3")),
            timeout_minutes=int(os.getenv("MIGRATION_TIMEOUT_MINUTES", "30"))
        )
        
        return MigrationConfig(
            postgres=postgres_config,
            mongodb=mongodb_config,
            control=control_config,
            execution=execution_config
        )
    
    def _parse_config(self, config_data: Dict[str, Any]) -> MigrationConfig:
        """Parse configuration data from Config Server format"""
        
        # Extract PostgreSQL configuration
        postgres_config = DatabaseConfig(
            url=config_data.get("migration.postgres.url", 
                               config_data.get("spring.datasource.url",
                                              "postgresql://financer_user:financer123@localhost:5432/financer_accounts")),
            username=config_data.get("migration.postgres.username",
                                   config_data.get("spring.datasource.username", "financer_user")),
            password=config_data.get("migration.postgres.password", 
                                   config_data.get("spring.datasource.password", "financer123")),
            schema=config_data.get("migration.postgres.schema", "public")
        )
        
        # Extract MongoDB configuration
        mongodb_config = MongoConfig(
            url=config_data.get("migration.mongodb.url", "mongodb://localhost:27017"),
            database=config_data.get("migration.mongodb.database", "financer")
        )
        
        # Extract control configuration
        control_config = ControlConfig(
            enabled=config_data.get("migration.control.enabled", True),
            table_name=config_data.get("migration.control.table-name", "migration_execution_history")
        )
        
        # Extract execution configuration
        execution_config = ExecutionConfig(
            generate_report=config_data.get("migration.execution.generate-report", True),
            report_format=config_data.get("migration.execution.report-format", "JSON"),
            max_retries=config_data.get("migration.execution.max-retries", 3),
            timeout_minutes=config_data.get("migration.execution.timeout-minutes", 30)
        )
        
        return MigrationConfig(
            postgres=postgres_config,
            mongodb=mongodb_config,
            control=control_config,
            execution=execution_config
        )


# Global configuration instance
_config_manager: Optional[ConfigManager] = None

def get_config_manager(profile: str = "local") -> ConfigManager:
    """Get or create the global configuration manager instance"""
    global _config_manager
    if _config_manager is None:
        _config_manager = ConfigManager(profile)
    return _config_manager

def get_migration_config(profile: str = "local") -> MigrationConfig:
    """Get the migration configuration for the specified profile"""
    manager = get_config_manager(profile)
    return manager.load_config()