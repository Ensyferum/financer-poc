"""
Financer Database Migration System - MongoDB Migration Engine
=============================================================

MongoDB migration engine for collection and document migrations.
"""

import os
import json
import hashlib
from datetime import datetime, timezone
from pathlib import Path
from typing import List, Dict, Any, Optional
from dataclasses import dataclass
import pymongo
from pymongo import MongoClient


@dataclass
class MongoMigrationScript:
    """Represents a single MongoDB migration script"""
    version: str
    description: str
    filename: str
    file_path: str
    checksum: str
    migration_data: Dict[str, Any]
    

@dataclass
class MongoMigrationResult:
    """Result of a MongoDB migration execution"""
    version: str
    description: str
    type: str = "NoSQL"
    state: str = "Success"
    script: Optional[str] = None
    checksum: Optional[str] = None
    execution_time: int = 0
    error_details: Optional[str] = None
    successful: bool = True
    operations_count: int = 0


class MongoDBMigrationEngine:
    """
    MongoDB migration engine for schema-less document migrations.
    Handles collection creation, indexing, and data transformations.
    """
    
    def __init__(self, mongodb_config, logger):
        self.config = mongodb_config
        self.logger = logger
        self.migration_collection = "migration_history"
        self._client = None
        self._database = None
        self.migrations_dir = Path("migrations/mongodb")
        
    def connect(self):
        """Establish connection to MongoDB"""
        try:
            self._client = MongoClient(self.config.url)
            self._database = self._client[self.config.database]
            
            # Test connection
            self._client.admin.command('ping')
            
            self.logger.log_database_connection("MongoDB", "SUCCESS")
            
        except Exception as e:
            self.logger.log_database_connection("MongoDB", "FAILED")
            raise Exception(f"Failed to connect to MongoDB: {e}")
    
    def disconnect(self):
        """Close connection to MongoDB"""
        if self._client:
            self._client.close()
            self._client = None
            self._database = None
    
    def initialize_migration_collection(self):
        """Create migration history collection with proper indexes"""
        if self._database is None:
            self.connect()
            
        try:
            # Create collection if it doesn't exist
            if self.migration_collection not in self._database.list_collection_names():
                self._database.create_collection(self.migration_collection)
                
                # Create indexes
                migration_coll = self._database[self.migration_collection]
                migration_coll.create_index("version", unique=True)
                migration_coll.create_index("installed_on")
                migration_coll.create_index("success")
                
                self.logger.log_table_creation(f"MongoDB collection: {self.migration_collection}")
            
            self.logger.get_logger("mongodb").info(f"✅ MongoDB migration collection '{self.migration_collection}' initialized")
            
        except Exception as e:
            self.logger.get_logger("mongodb").error(f"Failed to initialize migration collection: {e}")
            raise
    
    def discover_migrations(self) -> List[MongoMigrationScript]:
        """Discover and parse MongoDB migration scripts"""
        migrations = []
        
        if not self.migrations_dir.exists():
            self.logger.get_logger("mongodb").warning(f"MongoDB migrations directory not found: {self.migrations_dir}")
            return migrations
        
        # Look for JSON migration files
        for json_file in self.migrations_dir.glob("*.json"):
            try:
                # Read migration data
                migration_data = json.loads(json_file.read_text(encoding='utf-8'))
                
                # Validate required fields
                if 'version' not in migration_data or 'description' not in migration_data:
                    self.logger.get_logger("mongodb").warning(f"Invalid migration file {json_file}: missing version or description")
                    continue
                
                # Calculate checksum
                content = json_file.read_text(encoding='utf-8')
                checksum = self._calculate_checksum(content)
                
                migration = MongoMigrationScript(
                    version=migration_data['version'],
                    description=migration_data['description'],
                    filename=json_file.name,
                    file_path=str(json_file),
                    checksum=checksum,
                    migration_data=migration_data
                )
                
                migrations.append(migration)
                
            except json.JSONDecodeError as e:
                self.logger.get_logger("mongodb").error(f"Invalid JSON in migration file {json_file}: {e}")
            except Exception as e:
                self.logger.get_logger("mongodb").error(f"Error reading migration file {json_file}: {e}")
        
        # Sort migrations by version
        migrations.sort(key=lambda m: self._version_to_tuple(m.version))
        
        self.logger.get_logger("mongodb").info(f"📁 Discovered {len(migrations)} MongoDB migrations")
        return migrations
    
    def get_applied_migrations(self) -> List[Dict[str, Any]]:
        """Get list of already applied MongoDB migrations"""
        if self._database is None:
            self.connect()
            
        try:
            migration_coll = self._database[self.migration_collection]
            
            applied_migrations = list(migration_coll.find(
                {},
                {"_id": 0}  # Exclude MongoDB's _id field
            ).sort("installed_on", 1))
            
            # Convert datetime objects to ISO format
            for migration in applied_migrations:
                if 'installed_on' in migration and migration['installed_on']:
                    migration['installed_on'] = migration['installed_on'].isoformat()
            
            return applied_migrations
            
        except Exception as e:
            self.logger.get_logger("mongodb").error(f"Failed to get applied migrations: {e}")
            return []
    
    def get_pending_migrations(self) -> List[MongoMigrationScript]:
        """Get list of MongoDB migrations that haven't been applied yet"""
        all_migrations = self.discover_migrations()
        applied_migrations = self.get_applied_migrations()
        
        applied_versions = {m['version'] for m in applied_migrations}
        
        pending_migrations = [m for m in all_migrations if m.version not in applied_versions]
        
        self.logger.get_logger("mongodb").info(f"📋 Found {len(pending_migrations)} pending MongoDB migrations")
        return pending_migrations
    
    def execute_migrations(self) -> List[MongoMigrationResult]:
        """Execute all pending MongoDB migrations"""
        if self._database is None:
            self.connect()
            
        # Initialize migration collection if needed
        self.initialize_migration_collection()
        
        pending_migrations = self.get_pending_migrations()
        results = []
        
        if not pending_migrations:
            self.logger.get_logger("mongodb").info("📊 MongoDB collections are up to date. No migration necessary.")
            return results
        
        for migration in pending_migrations:
            self.logger.get_logger("mongodb").info(f"🔄 Executing MongoDB migration: {migration.version} - {migration.description}")
            
            start_time = datetime.now()
            result = self._execute_single_migration(migration)
            end_time = datetime.now()
            
            execution_time_ms = int((end_time - start_time).total_seconds() * 1000)
            result.execution_time = execution_time_ms
            
            self.logger.log_migration_execution(
                f"MongoDB {migration.version} - {migration.description}",
                "SUCCESS" if result.successful else "FAILED",
                execution_time_ms
            )
            
            results.append(result)
            
            # Stop on first failure
            if not result.successful:
                break
        
        return results
    
    def get_migration_info(self) -> List[Dict[str, Any]]:
        """Get comprehensive MongoDB migration information"""
        all_migrations = self.discover_migrations()
        applied_migrations = {m['version']: m for m in self.get_applied_migrations()}
        
        migration_info = []
        
        for migration in all_migrations:
            if migration.version in applied_migrations:
                applied = applied_migrations[migration.version]
                info = {
                    "version": migration.version,
                    "description": migration.description,
                    "type": "NoSQL",
                    "status": "Success" if applied.get('success', True) else "Failed",
                    "installed_on": applied.get('installed_on'),
                    "execution_time": applied.get('execution_time', 0),
                    "operations_count": applied.get('operations_count', 0)
                }
            else:
                info = {
                    "version": migration.version,
                    "description": migration.description,
                    "type": "NoSQL",
                    "status": "Pending",
                    "installed_on": None,
                    "execution_time": 0,
                    "operations_count": 0
                }
            
            migration_info.append(info)
        
        return migration_info
    
    def _execute_single_migration(self, migration: MongoMigrationScript) -> MongoMigrationResult:
        """Execute a single MongoDB migration"""
        operations_count = 0
        
        try:
            migration_data = migration.migration_data
            
            # Execute different types of operations
            if 'collections' in migration_data:
                operations_count += self._execute_collection_operations(migration_data['collections'])
            
            if 'indexes' in migration_data:
                operations_count += self._execute_index_operations(migration_data['indexes'])
            
            if 'data' in migration_data:
                operations_count += self._execute_data_operations(migration_data['data'])
            
            if 'aggregations' in migration_data:
                operations_count += self._execute_aggregation_operations(migration_data['aggregations'])
            
            # Record successful migration
            self._record_migration_success(migration, operations_count)
            
            return MongoMigrationResult(
                version=migration.version,
                description=migration.description,
                script=migration.filename,
                checksum=migration.checksum,
                successful=True,
                operations_count=operations_count
            )
            
        except Exception as e:
            error_msg = str(e)
            self.logger.get_logger("mongodb").error(f"MongoDB migration {migration.version} failed: {error_msg}")
            
            # Record failure
            self._record_migration_failure(migration, error_msg)
            
            return MongoMigrationResult(
                version=migration.version,
                description=migration.description,
                script=migration.filename,
                checksum=migration.checksum,
                error_details=error_msg,
                successful=False,
                state="Failed",
                operations_count=operations_count
            )
    
    def _execute_collection_operations(self, collections: List[Dict[str, Any]]) -> int:
        """Execute collection creation operations"""
        operations = 0
        
        for collection_def in collections:
            collection_name = collection_def['name']
            
            if collection_name not in self._database.list_collection_names():
                options = collection_def.get('options', {})
                self._database.create_collection(collection_name, **options)
                self.logger.get_logger("mongodb").info(f"📊 Created MongoDB collection: {collection_name}")
                operations += 1
        
        return operations
    
    def _execute_index_operations(self, indexes: List[Dict[str, Any]]) -> int:
        """Execute index creation operations"""
        operations = 0
        
        for index_def in indexes:
            collection_name = index_def['collection']
            index_spec = index_def['index']
            options = index_def.get('options', {})
            
            collection = self._database[collection_name]
            collection.create_index(list(index_spec.items()), **options)
            
            self.logger.get_logger("mongodb").info(f"🔍 Created index on {collection_name}: {index_spec}")
            operations += 1
        
        return operations
    
    def _execute_data_operations(self, data_operations: List[Dict[str, Any]]) -> int:
        """Execute data manipulation operations"""
        operations = 0
        
        for operation in data_operations:
            collection_name = operation['collection']
            operation_type = operation['type']
            
            collection = self._database[collection_name]
            
            if operation_type == 'insert':
                documents = operation['documents']
                if isinstance(documents, list):
                    result = collection.insert_many(documents)
                    operations += len(result.inserted_ids)
                else:
                    collection.insert_one(documents)
                    operations += 1
                    
            elif operation_type == 'update':
                filter_criteria = operation['filter']
                update_spec = operation['update']
                options = operation.get('options', {})
                
                result = collection.update_many(filter_criteria, update_spec, **options)
                operations += result.modified_count
                
            elif operation_type == 'delete':
                filter_criteria = operation['filter']
                result = collection.delete_many(filter_criteria)
                operations += result.deleted_count
        
        return operations
    
    def _execute_aggregation_operations(self, aggregations: List[Dict[str, Any]]) -> int:
        """Execute aggregation pipeline operations"""
        operations = 0
        
        for aggregation in aggregations:
            collection_name = aggregation['collection']
            pipeline = aggregation['pipeline']
            
            collection = self._database[collection_name]
            result = list(collection.aggregate(pipeline))
            operations += len(result)
        
        return operations
    
    def _record_migration_success(self, migration: MongoMigrationScript, operations_count: int):
        """Record successful migration in history collection"""
        self._record_migration(migration, True, 0, operations_count)
    
    def _record_migration_failure(self, migration: MongoMigrationScript, error_msg: str):
        """Record failed migration in history collection"""
        self._record_migration(migration, False, 0, 0, error_msg)
    
    def _record_migration(self, migration: MongoMigrationScript, success: bool, 
                         execution_time: int, operations_count: int, error_msg: str = None):
        """Record migration in history collection"""
        migration_coll = self._database[self.migration_collection]
        
        record = {
            "version": migration.version,
            "description": migration.description,
            "type": "NoSQL",
            "script": migration.filename,
            "checksum": migration.checksum,
            "installed_by": self._get_current_user(),
            "installed_on": datetime.now(timezone.utc),
            "execution_time": execution_time,
            "success": success,
            "operations_count": operations_count
        }
        
        if error_msg:
            record["error_message"] = error_msg
        
        migration_coll.insert_one(record)
    
    def _calculate_checksum(self, content: str) -> str:
        """Calculate MD5 checksum for MongoDB migration content"""
        return hashlib.md5(content.encode('utf-8')).hexdigest()
    
    def _version_to_tuple(self, version: str):
        """Convert version string to tuple for sorting"""
        try:
            return tuple(int(x) for x in version.split('.'))
        except ValueError:
            return (0,)
    
    def _get_current_user(self) -> str:
        """Get current system user"""
        import getpass
        try:
            return getpass.getuser()
        except:
            return "system"