"""
Financer Database Migration System - Execution Control
======================================================

Execution tracking and control system for migration history and audit.
"""

import uuid
import json
from datetime import datetime, timezone
from typing import Dict, Any, List, Optional
from dataclasses import dataclass, asdict
from enum import Enum
import psycopg2
from psycopg2.extras import RealDictCursor


class ExecutionStatus(Enum):
    """Migration execution status enumeration"""
    STARTED = "STARTED"
    IN_PROGRESS = "IN_PROGRESS"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


@dataclass
class MigrationExecution:
    """Migration execution record"""
    execution_id: str
    command: str
    environment: str
    status: ExecutionStatus
    start_time: datetime
    end_time: Optional[datetime] = None
    duration_seconds: int = 0
    executed_by: str = "system"
    error_message: Optional[str] = None
    postgres_migrations_count: int = 0
    mongodb_migrations_count: int = 0
    report_path: Optional[str] = None


class ExecutionControl:
    """
    Manages migration execution tracking and history in PostgreSQL.
    Provides audit trail and execution control functionality.
    """
    
    def __init__(self, postgres_config, logger):
        self.config = postgres_config
        self.logger = logger
        self.table_name = "migration_execution_history"
        self._connection = None
        
    def connect(self):
        """Establish connection to PostgreSQL"""
        try:
            # Parse PostgreSQL URL or use individual components
            if self.config.url.startswith('postgresql://'):
                self._connection = psycopg2.connect(self.config.url)
            else:
                self._connection = psycopg2.connect(
                    host=self.config.url.replace('jdbc:postgresql://', '').split(':')[0],
                    port=int(self.config.url.split(':')[-1].split('/')[0]),
                    database=self.config.url.split('/')[-1],
                    user=self.config.username,
                    password=self.config.password
                )
            
            self._connection.autocommit = True
            self.logger.log_database_connection("PostgreSQL (Control)", "SUCCESS")
            
        except Exception as e:
            self.logger.log_database_connection("PostgreSQL (Control)", "FAILED")
            raise Exception(f"Failed to connect to PostgreSQL for execution control: {e}")
    
    def disconnect(self):
        """Close connection to PostgreSQL"""
        if self._connection:
            self._connection.close()
            self._connection = None
    
    def initialize_control_table(self):
        """Create the migration execution history table if it doesn't exist"""
        if not self._connection:
            self.connect()
            
        create_table_sql = f"""
        CREATE TABLE IF NOT EXISTS {self.table_name} (
            execution_id UUID PRIMARY KEY,
            command VARCHAR(100) NOT NULL,
            environment VARCHAR(50) NOT NULL,
            status VARCHAR(20) NOT NULL,
            start_time TIMESTAMP WITH TIME ZONE NOT NULL,
            end_time TIMESTAMP WITH TIME ZONE,
            duration_seconds INTEGER DEFAULT 0,
            executed_by VARCHAR(255) DEFAULT 'system',
            error_message TEXT,
            postgres_migrations_count INTEGER DEFAULT 0,
            mongodb_migrations_count INTEGER DEFAULT 0,
            report_path TEXT,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
        );
        
        -- Create indexes for better query performance
        CREATE INDEX IF NOT EXISTS idx_migration_exec_status 
        ON {self.table_name} (status);
        
        CREATE INDEX IF NOT EXISTS idx_migration_exec_start_time 
        ON {self.table_name} (start_time DESC);
        
        CREATE INDEX IF NOT EXISTS idx_migration_exec_environment 
        ON {self.table_name} (environment);
        """
        
        try:
            with self._connection.cursor() as cursor:
                cursor.execute(create_table_sql)
            
            self.logger.log_table_creation(self.table_name)
            self.logger.get_logger("control").info(f"✅ Migration control table '{self.table_name}' initialized successfully")
            
        except Exception as e:
            self.logger.get_logger("control").error(f"Failed to initialize control table: {e}")
            raise
    
    def start_execution(self, command: str, environment: str) -> str:
        """Start a new migration execution and return execution ID"""
        execution_id = str(uuid.uuid4())
        executed_by = self._get_current_user()
        
        execution = MigrationExecution(
            execution_id=execution_id,
            command=command,
            environment=environment,
            status=ExecutionStatus.STARTED,
            start_time=datetime.now(timezone.utc),
            executed_by=executed_by
        )
        
        self._insert_execution(execution)
        
        self.logger.get_logger("control").info(f"🚀 Started migration execution: {execution_id}")
        return execution_id
    
    def update_execution_progress(self, execution_id: str, 
                                 postgres_count: int = 0, 
                                 mongodb_count: int = 0):
        """Update execution progress with migration counts"""
        if not self._connection:
            self.connect()
            
        update_sql = f"""
        UPDATE {self.table_name} 
        SET postgres_migrations_count = %s,
            mongodb_migrations_count = %s,
            status = %s
        WHERE execution_id = %s
        """
        
        try:
            with self._connection.cursor() as cursor:
                cursor.execute(update_sql, (
                    postgres_count, 
                    mongodb_count, 
                    ExecutionStatus.IN_PROGRESS.value,
                    execution_id
                ))
            
            self.logger.get_logger("control").debug(f"📊 Updated execution progress: {execution_id}")
            
        except Exception as e:
            self.logger.get_logger("control").error(f"Failed to update execution progress: {e}")
            raise
    
    def complete_execution(self, execution_id: str, status: ExecutionStatus, 
                          error_message: Optional[str] = None,
                          report_path: Optional[str] = None):
        """Complete a migration execution with final status"""
        if not self._connection:
            self.connect()
            
        end_time = datetime.now(timezone.utc)
        
        # Calculate duration
        duration_sql = f"""
        SELECT EXTRACT(EPOCH FROM (%s - start_time))::INTEGER as duration_seconds
        FROM {self.table_name} 
        WHERE execution_id = %s
        """
        
        update_sql = f"""
        UPDATE {self.table_name} 
        SET status = %s,
            end_time = %s,
            duration_seconds = %s,
            error_message = %s,
            report_path = %s
        WHERE execution_id = %s
        """
        
        try:
            with self._connection.cursor() as cursor:
                # Get duration
                cursor.execute(duration_sql, (end_time, execution_id))
                result = cursor.fetchone()
                duration_seconds = result[0] if result else 0
                
                # Update execution
                cursor.execute(update_sql, (
                    status.value,
                    end_time,
                    duration_seconds,
                    error_message,
                    report_path,
                    execution_id
                ))
            
            status_emoji = "✅" if status == ExecutionStatus.SUCCESS else "❌"
            self.logger.get_logger("control").info(
                f"{status_emoji} Completed migration execution: {execution_id} "
                f"with status: {status.value}"
            )
            
            return duration_seconds
            
        except Exception as e:
            # If table doesn't exist (e.g., after clean), just return 0 duration
            if "does not exist" in str(e):
                self.logger.get_logger("control").info("Control table not found - execution tracking skipped")
                return 0
            else:
                self.logger.get_logger("control").error(f"Failed to complete execution: {e}")
                raise
    
    def get_execution_history(self, limit: int = 50) -> List[Dict[str, Any]]:
        """Get migration execution history"""
        if not self._connection:
            self.connect()
            
        query_sql = f"""
        SELECT execution_id, command, environment, status, 
               start_time, end_time, duration_seconds, executed_by,
               error_message, postgres_migrations_count, mongodb_migrations_count,
               report_path
        FROM {self.table_name}
        ORDER BY start_time DESC
        LIMIT %s
        """
        
        try:
            with self._connection.cursor(cursor_factory=RealDictCursor) as cursor:
                cursor.execute(query_sql, (limit,))
                results = cursor.fetchall()
                
                # Convert to list of dictionaries and handle datetime serialization
                history = []
                for row in results:
                    row_dict = dict(row)
                    # Convert datetime objects to ISO format strings
                    for key, value in row_dict.items():
                        if isinstance(value, datetime):
                            row_dict[key] = value.isoformat()
                    history.append(row_dict)
                
                return history
                
        except Exception as e:
            self.logger.get_logger("control").error(f"Failed to get execution history: {e}")
            raise
    
    def get_execution_by_id(self, execution_id: str) -> Optional[Dict[str, Any]]:
        """Get specific execution by ID"""
        if not self._connection:
            self.connect()
            
        query_sql = f"""
        SELECT * FROM {self.table_name}
        WHERE execution_id = %s
        """
        
        try:
            with self._connection.cursor(cursor_factory=RealDictCursor) as cursor:
                cursor.execute(query_sql, (execution_id,))
                result = cursor.fetchone()
                
                if result:
                    row_dict = dict(result)
                    # Convert datetime objects to ISO format strings
                    for key, value in row_dict.items():
                        if isinstance(value, datetime):
                            row_dict[key] = value.isoformat()
                    return row_dict
                
                return None
                
        except Exception as e:
            self.logger.get_logger("control").error(f"Failed to get execution by ID: {e}")
            raise
    
    def _insert_execution(self, execution: MigrationExecution):
        """Insert new execution record"""
        if not self._connection:
            self.connect()
            
        insert_sql = f"""
        INSERT INTO {self.table_name} 
        (execution_id, command, environment, status, start_time, executed_by)
        VALUES (%s, %s, %s, %s, %s, %s)
        """
        
        try:
            with self._connection.cursor() as cursor:
                cursor.execute(insert_sql, (
                    execution.execution_id,
                    execution.command,
                    execution.environment,
                    execution.status.value,
                    execution.start_time,
                    execution.executed_by
                ))
                
        except Exception as e:
            self.logger.get_logger("control").error(f"Failed to insert execution record: {e}")
            raise
    
    def _get_current_user(self) -> str:
        """Get current system user"""
        import getpass
        import platform
        
        try:
            username = getpass.getuser()
            hostname = platform.node()
            return f"{username}@{hostname}"
        except:
            return "system"
    
    def clean_database_schemas(self, confirm: bool = False) -> bool:
        """
        Clean database schemas - DANGEROUS OPERATION!
        This will drop all tables and collections.
        
        Args:
            confirm: Must be True to actually execute the operation
            
        Returns:
            bool: True if operation was executed, False if cancelled
        """
        if not confirm:
            self.logger.get_logger("control").warning("Clean operation cancelled - confirmation required")
            return False
            
        if not self._connection:
            self.connect()
        
        try:
            self.logger.get_logger("control").warning("🚨 DANGER: Starting database cleanup operation")
            
            # Drop migration control table
            drop_control_sql = f"DROP TABLE IF EXISTS {self.table_name} CASCADE"
            
            # Drop Flyway schema history table
            drop_flyway_sql = "DROP TABLE IF EXISTS flyway_schema_history CASCADE"
            
            # Drop accounts and related tables
            drop_tables_sql = """
            DROP TABLE IF EXISTS account_audit CASCADE;
            DROP TABLE IF EXISTS accounts CASCADE;
            DROP SEQUENCE IF EXISTS account_number_seq CASCADE;
            """
            
            with self._connection.cursor() as cursor:
                # Execute drops
                cursor.execute(drop_control_sql)
                cursor.execute(drop_flyway_sql)
                cursor.execute(drop_tables_sql)
            
            self.logger.get_logger("control").warning("💥 PostgreSQL tables dropped successfully")
            
            return True
            
        except Exception as e:
            self.logger.get_logger("control").error(f"Failed to clean database schemas: {e}")
            raise