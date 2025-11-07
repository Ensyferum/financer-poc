"""
Financer Database Migration System - PostgreSQL Migration Engine
================================================================

PostgreSQL migration engine with Flyway-compatible functionality.
"""

import os
import re
import hashlib
from datetime import datetime, timezone
from pathlib import Path
from typing import List, Dict, Any, Optional, Tuple
from dataclasses import dataclass
import psycopg2
from psycopg2.extras import RealDictCursor


@dataclass
class MigrationScript:
    """Represents a single migration script"""
    version: str
    description: str
    filename: str
    file_path: str
    checksum: str
    sql_content: str
    

@dataclass
class MigrationResult:
    """Result of a migration execution"""
    version: str
    description: str
    type: str = "SQL"
    state: str = "Success"
    script: Optional[str] = None
    checksum: Optional[str] = None
    execution_time: int = 0
    error_details: Optional[str] = None
    successful: bool = True


class PostgreSQLMigrationEngine:
    """
    PostgreSQL migration engine with Flyway-compatible schema versioning.
    Handles SQL script execution and version tracking.
    """
    
    def __init__(self, postgres_config, logger):
        self.config = postgres_config
        self.logger = logger
        self.schema_history_table = "flyway_schema_history"
        self._connection = None
        self.migrations_dir = Path("migrations/postgresql")
        
    def connect(self):
        """Establish connection to PostgreSQL"""
        try:
            # Parse PostgreSQL URL format
            if self.config.url.startswith('postgresql://'):
                self._connection = psycopg2.connect(self.config.url)
            else:
                # Handle jdbc:postgresql:// format
                clean_url = self.config.url.replace('jdbc:postgresql://', '')
                host_port, database = clean_url.split('/')
                host, port = host_port.split(':')
                
                self._connection = psycopg2.connect(
                    host=host,
                    port=int(port),
                    database=database,
                    user=self.config.username,
                    password=self.config.password
                )
            
            self._connection.autocommit = True
            self.logger.log_database_connection("PostgreSQL", "SUCCESS")
            
        except Exception as e:
            self.logger.log_database_connection("PostgreSQL", "FAILED")
            raise Exception(f"Failed to connect to PostgreSQL: {e}")
    
    def disconnect(self):
        """Close connection to PostgreSQL"""
        if self._connection:
            self._connection.close()
            self._connection = None
    
    def initialize_schema_history(self):
        """Create Flyway-compatible schema history table"""
        if not self._connection:
            self.connect()
            
        create_table_sql = f"""
        CREATE TABLE IF NOT EXISTS {self.schema_history_table} (
            installed_rank INTEGER NOT NULL,
            version VARCHAR(50),
            description VARCHAR(200) NOT NULL,
            type VARCHAR(20) NOT NULL,
            script VARCHAR(1000) NOT NULL,
            checksum INTEGER,
            installed_by VARCHAR(100) NOT NULL,
            installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            execution_time INTEGER NOT NULL,
            success BOOLEAN NOT NULL
        );
        
        -- Create unique index on installed_rank
        CREATE UNIQUE INDEX IF NOT EXISTS {self.schema_history_table}_ir_idx 
        ON {self.schema_history_table} (installed_rank);
        
        -- Create index on version for faster lookups
        CREATE INDEX IF NOT EXISTS {self.schema_history_table}_version_idx 
        ON {self.schema_history_table} (version);
        """
        
        try:
            with self._connection.cursor() as cursor:
                cursor.execute(create_table_sql)
            
            self.logger.log_table_creation(self.schema_history_table)
            self.logger.get_logger("postgres").info(f"✅ Schema history table '{self.schema_history_table}' initialized")
            
        except Exception as e:
            self.logger.get_logger("postgres").error(f"Failed to initialize schema history: {e}")
            raise
    
    def discover_migrations(self) -> List[MigrationScript]:
        """Discover and parse migration scripts from the migrations directory"""
        migrations = []
        
        if not self.migrations_dir.exists():
            self.logger.get_logger("postgres").warning(f"Migrations directory not found: {self.migrations_dir}")
            return migrations
        
        # Pattern for Flyway-compatible migration files: V{version}__{description}.sql
        version_pattern = re.compile(r'^V([0-9]+(?:\.[0-9]+)*)__(.+)\.sql$')
        
        for sql_file in self.migrations_dir.glob("*.sql"):
            match = version_pattern.match(sql_file.name)
            if match:
                version = match.group(1)
                description = match.group(2).replace('_', ' ')
                
                try:
                    # Read SQL content
                    sql_content = sql_file.read_text(encoding='utf-8')
                    
                    # Calculate checksum (CRC32 compatible with Flyway)
                    checksum = self._calculate_checksum(sql_content)
                    
                    migration = MigrationScript(
                        version=version,
                        description=description,
                        filename=sql_file.name,
                        file_path=str(sql_file),
                        checksum=checksum,
                        sql_content=sql_content
                    )
                    
                    migrations.append(migration)
                    
                except Exception as e:
                    self.logger.get_logger("postgres").error(f"Error reading migration file {sql_file}: {e}")
        
        # Sort migrations by version
        migrations.sort(key=lambda m: self._version_to_tuple(m.version))
        
        self.logger.get_logger("postgres").info(f"📁 Discovered {len(migrations)} PostgreSQL migrations")
        return migrations
    
    def get_applied_migrations(self) -> List[Dict[str, Any]]:
        """Get list of already applied migrations from schema history"""
        if not self._connection:
            self.connect()
            
        query_sql = f"""
        SELECT version, description, type, script, checksum, 
               installed_by, installed_on, execution_time, success
        FROM {self.schema_history_table}
        ORDER BY installed_rank
        """
        
        try:
            with self._connection.cursor(cursor_factory=RealDictCursor) as cursor:
                cursor.execute(query_sql)
                results = cursor.fetchall()
                
                applied_migrations = []
                for row in results:
                    row_dict = dict(row)
                    # Convert datetime to ISO format
                    if 'installed_on' in row_dict and row_dict['installed_on']:
                        row_dict['installed_on'] = row_dict['installed_on'].isoformat()
                    applied_migrations.append(row_dict)
                
                return applied_migrations
                
        except psycopg2.Error as e:
            if "does not exist" in str(e):
                # Table doesn't exist yet, return empty list
                return []
            raise
    
    def get_pending_migrations(self) -> List[MigrationScript]:
        """Get list of migrations that haven't been applied yet"""
        all_migrations = self.discover_migrations()
        applied_migrations = self.get_applied_migrations()
        
        applied_versions = {m['version'] for m in applied_migrations if m['version']}
        
        pending_migrations = [m for m in all_migrations if m.version not in applied_versions]
        
        self.logger.get_logger("postgres").info(f"📋 Found {len(pending_migrations)} pending PostgreSQL migrations")
        return pending_migrations
    
    def execute_migrations(self) -> List[MigrationResult]:
        """Execute all pending migrations"""
        if not self._connection:
            self.connect()
            
        # Initialize schema history if needed
        self.initialize_schema_history()
        
        pending_migrations = self.get_pending_migrations()
        results = []
        
        if not pending_migrations:
            self.logger.get_logger("postgres").info("📊 PostgreSQL schema is up to date. No migration necessary.")
            return results
        
        for migration in pending_migrations:
            self.logger.get_logger("postgres").info(f"🔄 Executing migration: {migration.version} - {migration.description}")
            
            start_time = datetime.now()
            result = self._execute_single_migration(migration)
            end_time = datetime.now()
            
            execution_time_ms = int((end_time - start_time).total_seconds() * 1000)
            result.execution_time = execution_time_ms
            
            self.logger.log_migration_execution(
                f"{migration.version} - {migration.description}",
                "SUCCESS" if result.successful else "FAILED",
                execution_time_ms
            )
            
            results.append(result)
            
            # Stop on first failure
            if not result.successful:
                break
        
        return results
    
    def validate_migrations(self) -> bool:
        """Validate that applied migrations match their checksums"""
        if not self._connection:
            self.connect()
            
        applied_migrations = self.get_applied_migrations()
        current_migrations = {m.version: m for m in self.discover_migrations()}
        
        validation_passed = True
        
        for applied in applied_migrations:
            version = applied['version']
            if version in current_migrations:
                current_checksum = current_migrations[version].checksum
                applied_checksum = applied['checksum']
                
                if current_checksum != applied_checksum:
                    self.logger.get_logger("postgres").error(
                        f"❌ Checksum mismatch for migration {version}: "
                        f"applied={applied_checksum}, current={current_checksum}"
                    )
                    validation_passed = False
        
        if validation_passed:
            self.logger.get_logger("postgres").info("✅ All applied migrations validated successfully")
        
        return validation_passed
    
    def get_migration_info(self) -> List[Dict[str, Any]]:
        """Get comprehensive migration information combining applied and pending"""
        all_migrations = self.discover_migrations()
        applied_migrations = {m['version']: m for m in self.get_applied_migrations()}
        
        migration_info = []
        
        for migration in all_migrations:
            if migration.version in applied_migrations:
                applied = applied_migrations[migration.version]
                info = {
                    "version": migration.version,
                    "description": migration.description,
                    "type": "SQL",
                    "status": "Success" if applied['success'] else "Failed",
                    "installed_on": applied.get('installed_on'),
                    "execution_time": applied.get('execution_time', 0)
                }
            else:
                info = {
                    "version": migration.version,
                    "description": migration.description,
                    "type": "SQL",
                    "status": "Pending",
                    "installed_on": None,
                    "execution_time": 0
                }
            
            migration_info.append(info)
        
        return migration_info
    
    def _execute_single_migration(self, migration: MigrationScript) -> MigrationResult:
        """Execute a single migration script"""
        try:
            with self._connection.cursor() as cursor:
                # Execute the migration SQL
                cursor.execute(migration.sql_content)
                
                # Record in schema history
                self._record_migration_success(migration)
                
                return MigrationResult(
                    version=migration.version,
                    description=migration.description,
                    script=migration.filename,
                    checksum=migration.checksum,
                    successful=True
                )
                
        except Exception as e:
            error_msg = str(e)
            self.logger.get_logger("postgres").error(f"Migration {migration.version} failed: {error_msg}")
            
            # Record failure in schema history
            self._record_migration_failure(migration, error_msg)
            
            return MigrationResult(
                version=migration.version,
                description=migration.description,
                script=migration.filename,
                checksum=migration.checksum,
                error_details=error_msg,
                successful=False,
                state="Failed"
            )
    
    def _record_migration_success(self, migration: MigrationScript):
        """Record successful migration in schema history"""
        self._record_migration(migration, True, 0)
    
    def _record_migration_failure(self, migration: MigrationScript, error_msg: str):
        """Record failed migration in schema history"""
        self._record_migration(migration, False, 0)
    
    def _record_migration(self, migration: MigrationScript, success: bool, execution_time: int):
        """Record migration in schema history table"""
        installed_by = self._get_current_user()
        
        # Get next rank
        rank_sql = f"SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM {self.schema_history_table}"
        
        insert_sql = f"""
        INSERT INTO {self.schema_history_table} 
        (installed_rank, version, description, type, script, checksum, 
         installed_by, installed_on, execution_time, success)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """
        
        with self._connection.cursor() as cursor:
            cursor.execute(rank_sql)
            next_rank = cursor.fetchone()[0]
            
            cursor.execute(insert_sql, (
                next_rank,
                migration.version,
                migration.description,
                "SQL",
                migration.filename,
                int(migration.checksum),
                installed_by,
                datetime.now(timezone.utc),
                execution_time,
                success
            ))
    
    def _calculate_checksum(self, content: str) -> str:
        """Calculate CRC32 checksum compatible with Flyway"""
        import zlib
        # Remove line endings for consistent checksums across platforms
        normalized_content = content.replace('\r\n', '\n').replace('\r', '\n')
        crc32 = zlib.crc32(normalized_content.encode('utf-8'))
        # Convert to signed 32-bit integer like Flyway
        if crc32 >= 2**31:
            crc32 -= 2**32
        return str(crc32)
    
    def _version_to_tuple(self, version: str) -> Tuple[int, ...]:
        """Convert version string to tuple for sorting"""
        try:
            return tuple(int(x) for x in version.split('.'))
        except ValueError:
            # Fallback for non-numeric versions
            return (0,)
    
    def _get_current_user(self) -> str:
        """Get current system user"""
        import getpass
        try:
            return getpass.getuser()
        except:
            return "system"