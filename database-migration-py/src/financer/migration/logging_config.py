"""
Financer Database Migration System - Logging Configuration
==========================================================

Centralized logging system following the project's standards,
with execution-specific log files and detailed formatting.
"""

import os
import logging
import logging.handlers
from datetime import datetime
from typing import Optional
import uuid
from pathlib import Path
import colorlog


class MigrationLogger:
    """
    Centralized logging configuration for the migration system.
    Creates execution-specific log files and provides structured logging.
    """
    
    def __init__(self, execution_id: Optional[str] = None, log_level: str = "INFO"):
        self.execution_id = execution_id or str(uuid.uuid4())
        self.log_level = getattr(logging, log_level.upper(), logging.INFO)
        self.log_dir = Path("logs/database-migration")
        self.report_dir = Path("logs/migration-reports")
        
        # Create log directories
        self.log_dir.mkdir(parents=True, exist_ok=True)
        self.report_dir.mkdir(parents=True, exist_ok=True)
        
        # Initialize loggers
        self._setup_loggers()
        
    def _setup_loggers(self):
        """Setup all loggers with appropriate handlers and formatters"""
        
        # Main logger
        self.logger = logging.getLogger(f"financer.migration.{self.execution_id}")
        self.logger.setLevel(self.log_level)
        self.logger.handlers.clear()  # Clear any existing handlers
        
        # Console handler with colors
        console_handler = colorlog.StreamHandler()
        console_formatter = colorlog.ColoredFormatter(
            "%(log_color)s%(asctime)s [%(levelname)s] [%(name)s] - %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S",
            log_colors={
                'DEBUG': 'cyan',
                'INFO': 'green',
                'WARNING': 'yellow',
                'ERROR': 'red',
                'CRITICAL': 'red,bg_white',
            }
        )
        console_handler.setFormatter(console_formatter)
        self.logger.addHandler(console_handler)
        
        # Main file handler
        main_log_file = self.log_dir / "database-migration.log"
        file_handler = logging.handlers.RotatingFileHandler(
            main_log_file,
            maxBytes=5 * 1024 * 1024,  # 5MB
            backupCount=10,
            encoding='utf-8'
        )
        file_formatter = logging.Formatter(
            "%(asctime)s [%(levelname)s] [%(name)s] - %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S"
        )
        file_handler.setFormatter(file_formatter)
        self.logger.addHandler(file_handler)
        
        # Execution-specific log file
        timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
        execution_log_file = self.log_dir / f"migration-execution-{timestamp}-{self.execution_id[:8]}.log"
        execution_handler = logging.FileHandler(execution_log_file, encoding='utf-8')
        execution_handler.setFormatter(file_formatter)
        self.logger.addHandler(execution_handler)
        
        # Error log file
        error_log_file = self.log_dir / "database-migration-error.log"
        error_handler = logging.handlers.RotatingFileHandler(
            error_log_file,
            maxBytes=5 * 1024 * 1024,
            backupCount=5,
            encoding='utf-8'
        )
        error_handler.setLevel(logging.ERROR)
        error_handler.setFormatter(file_formatter)
        self.logger.addHandler(error_handler)
        
        # Store file paths for reference
        self.execution_log_file = execution_log_file
        self.main_log_file = main_log_file
        self.error_log_file = error_log_file
        
    def get_logger(self, name: str = "") -> logging.Logger:
        """Get a logger instance for a specific component"""
        if name:
            logger_name = f"financer.migration.{name}"
        else:
            logger_name = f"financer.migration"
            
        logger = logging.getLogger(logger_name)
        logger.setLevel(self.log_level)
        
        # If this logger doesn't have handlers, inherit from main logger
        if not logger.handlers:
            logger.parent = self.logger
            
        return logger
    
    def log_execution_start(self, command: str, environment: str):
        """Log the start of a migration execution"""
        self.logger.info("🚀 Starting Financer Database Migration Tool v2.0.0")
        self.logger.info("=" * 60)
        self.logger.info(f"Execution ID: {self.execution_id}")
        self.logger.info(f"Command: {command}")
        self.logger.info(f"Environment: {environment}")
        self.logger.info(f"Timestamp: {datetime.now().isoformat()}")
        self.logger.info("=" * 60)
    
    def log_execution_end(self, status: str, duration_seconds: int):
        """Log the end of a migration execution"""
        self.logger.info("=" * 60)
        self.logger.info(f"Migration execution completed")
        self.logger.info(f"Status: {status}")
        self.logger.info(f"Duration: {duration_seconds} seconds")
        self.logger.info(f"Logs written to: {self.execution_log_file}")
        if status == "SUCCESS":
            self.logger.info("✅ Migration completed successfully!")
        else:
            self.logger.error("❌ Migration failed!")
        self.logger.info("=" * 60)
    
    def log_config_loading(self, source: str):
        """Log configuration loading information"""
        self.logger.info(f"📁 Loading configuration from: {source}")
    
    def log_database_connection(self, database: str, status: str):
        """Log database connection status"""
        if status == "SUCCESS":
            self.logger.info(f"🔗 Connected to {database} successfully")
        else:
            self.logger.error(f"❌ Failed to connect to {database}")
    
    def log_migration_execution(self, migration_name: str, status: str, duration_ms: int = 0):
        """Log individual migration execution"""
        if status == "SUCCESS":
            self.logger.info(f"✅ Migration '{migration_name}' completed successfully ({duration_ms}ms)")
        else:
            self.logger.error(f"❌ Migration '{migration_name}' failed")
    
    def log_table_creation(self, table_name: str):
        """Log table creation"""
        self.logger.info(f"📊 Created table: {table_name}")
    
    def log_report_generation(self, report_path: str):
        """Log report generation"""
        self.logger.info(f"📋 Migration report generated: {report_path}")


# Global logger instance
_migration_logger: Optional[MigrationLogger] = None

def get_migration_logger(execution_id: Optional[str] = None, log_level: str = "INFO") -> MigrationLogger:
    """Get or create the global migration logger instance"""
    global _migration_logger
    if _migration_logger is None or (execution_id and _migration_logger.execution_id != execution_id):
        _migration_logger = MigrationLogger(execution_id, log_level)
    return _migration_logger

def get_logger(name: str = "", execution_id: Optional[str] = None) -> logging.Logger:
    """Get a logger instance for a specific component"""
    migration_logger = get_migration_logger(execution_id)
    return migration_logger.get_logger(name)