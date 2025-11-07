#!/usr/bin/env python3
"""
Financer Database Migration System - Main Entry Point
=====================================================

Serverless Python migration system with Config Server integration,
execution tracking, and comprehensive reporting.

Usage:
    python migrate.py [command] [--environment ENV] [--profile PROFILE]
    
Commands:
    migrate   - Execute all pending migrations
    info      - Show migration status and information
    validate  - Validate migration checksums
    history   - Show execution history
    clean     - Clean database schemas (DANGER!)

Options:
    --environment, -e   Environment (local, docker, prod)
    --profile, -p       Configuration profile  
    --verbose, -v       Enable verbose logging
    --help, -h          Show this help message
"""

import sys
import argparse
import os
from datetime import datetime, timezone
from pathlib import Path

# Add src directory to Python path
sys.path.insert(0, str(Path(__file__).parent / "src"))

from financer.migration.config import get_migration_config
from financer.migration.logging_config import get_migration_logger
from financer.migration.execution_control import ExecutionControl, ExecutionStatus
from financer.migration.postgres_engine import PostgreSQLMigrationEngine
from financer.migration.mongodb_engine import MongoDBMigrationEngine
from financer.migration.report_generator import ReportGenerator


class MigrationRunner:
    """
    Main migration runner orchestrating all migration operations.
    Serverless implementation maintaining all enterprise features.
    """
    
    def __init__(self, environment: str = "local", verbose: bool = False):
        self.environment = environment
        self.verbose = verbose
        
        # Initialize logging
        log_level = "DEBUG" if verbose else "INFO"
        self.migration_logger = get_migration_logger(log_level=log_level)
        self.logger = self.migration_logger.get_logger("runner")
        
        # Initialize configuration
        self.config = get_migration_config(environment)
        
        # Initialize components
        self.execution_control = ExecutionControl(self.config.postgres, self.migration_logger)
        self.postgres_engine = PostgreSQLMigrationEngine(self.config.postgres, self.migration_logger) 
        self.mongodb_engine = MongoDBMigrationEngine(self.config.mongodb, self.migration_logger)
        self.report_generator = ReportGenerator(self.migration_logger)
        
        # Execution tracking
        self.execution_id = None
        self.start_time = None
        
    def run(self, command: str) -> int:
        """Main execution method"""
        try:
            self.start_time = datetime.now(timezone.utc)
            
            # Display welcome message
            self.report_generator.display_welcome_message()
            
            # Initialize execution control table first
            self.execution_control.connect()
            self.execution_control.initialize_control_table()
            
            # Start execution tracking
            self.execution_id = self.execution_control.start_execution(command, self.environment)
            self.migration_logger.log_execution_start(command, self.environment)
            
            # Execute command
            result = self._execute_command(command)
            
            # Complete execution tracking
            end_time = datetime.now(timezone.utc)
            status = ExecutionStatus.SUCCESS if result == 0 else ExecutionStatus.FAILED
            
            duration_seconds = self.execution_control.complete_execution(
                self.execution_id, status
            )
            
            self.migration_logger.log_execution_end(status.value, duration_seconds)
            
            return result
            
        except KeyboardInterrupt:
            self.logger.warning("⚠️ Migration interrupted by user")
            self._handle_execution_failure("Migration interrupted by user")
            return 130  # Standard exit code for SIGINT
            
        except Exception as e:
            error_msg = f"Migration failed: {str(e)}"
            self.logger.error(f"❌ {error_msg}")
            self._handle_execution_failure(error_msg)
            return 1
        
        finally:
            # Cleanup connections
            self._cleanup()
    
    def _execute_command(self, command: str) -> int:
        """Execute the specified migration command"""
        
        if command == "migrate":
            return self._execute_migrate()
        elif command == "info":
            return self._execute_info()
        elif command == "validate":
            return self._execute_validate()
        elif command == "history":
            return self._execute_history()
        elif command == "clean":
            return self._execute_clean()
        else:
            self.report_generator.display_error_message(f"Unknown command: {command}")
            self._show_help()
            return 1
    
    def _execute_migrate(self) -> int:
        """Execute all pending migrations"""
        self.logger.info("🔄 Executing migrations...")
        
        try:
            # Connect to databases
            self.execution_control.connect()
            self.postgres_engine.connect()
            self.mongodb_engine.connect()
            
            # Initialize control systems
            self.execution_control.initialize_control_table()
            
            # Execute PostgreSQL migrations
            self.logger.info("📊 Executing PostgreSQL migrations...")
            postgres_results = self.postgres_engine.execute_migrations()
            
            # Execute MongoDB migrations
            self.logger.info("📊 Executing MongoDB migrations...")
            mongodb_results = self.mongodb_engine.execute_migrations()
            
            # Update execution progress
            self.execution_control.update_execution_progress(
                self.execution_id,
                len(postgres_results),
                len(mongodb_results)
            )
            
            # Check for failures
            postgres_failed = any(not r.successful for r in postgres_results)
            mongodb_failed = any(not r.successful for r in mongodb_results)
            
            if postgres_failed or mongodb_failed:
                self.report_generator.display_error_message("Some migrations failed")
                self._generate_execution_report("FAILED", postgres_results, mongodb_results)
                return 1
            
            # Generate success report
            self.report_generator.display_success_message("All migrations completed successfully")
            self._generate_execution_report("SUCCESS", postgres_results, mongodb_results)
            
            return 0
            
        except Exception as e:
            self.logger.error(f"Migration execution failed: {e}")
            self._generate_execution_report("FAILED", [], [], str(e))
            raise
    
    def _execute_info(self) -> int:
        """Show migration status information"""
        self.logger.info("📋 Retrieving migration status...")
        
        try:
            # Connect to databases
            self.postgres_engine.connect()
            self.mongodb_engine.connect()
            
            # Get migration information
            postgres_info = self.postgres_engine.get_migration_info()
            mongodb_info = self.mongodb_engine.get_migration_info()
            
            # Display information tables
            self.report_generator.display_migration_info_table(postgres_info, mongodb_info)
            
            # Generate report
            self._generate_execution_report("SUCCESS", postgres_info, mongodb_info)
            
            return 0
            
        except Exception as e:
            self.logger.error(f"Failed to retrieve migration info: {e}")
            self._generate_execution_report("FAILED", [], [], str(e))
            raise
    
    def _execute_validate(self) -> int:
        """Validate migration checksums"""
        self.logger.info("🔍 Validating migrations...")
        
        try:
            # Connect to databases
            self.postgres_engine.connect()
            
            # Validate PostgreSQL migrations
            postgres_valid = self.postgres_engine.validate_migrations()
            
            if postgres_valid:
                self.report_generator.display_success_message("All migrations validated successfully")
                self._generate_execution_report("SUCCESS", [], [])
                return 0
            else:
                self.report_generator.display_error_message("Migration validation failed")
                self._generate_execution_report("FAILED", [], [], "Validation failed")
                return 1
                
        except Exception as e:
            self.logger.error(f"Validation failed: {e}")
            self._generate_execution_report("FAILED", [], [], str(e))
            raise
    
    def _execute_history(self) -> int:
        """Show execution history"""
        self.logger.info("📚 Retrieving execution history...")
        
        try:
            # Connect to control database
            self.execution_control.connect()
            self.execution_control.initialize_control_table()
            
            # Get execution history
            history = self.execution_control.get_execution_history(limit=20)
            
            # Display history
            self.report_generator.display_execution_history(history)
            
            self._generate_execution_report("SUCCESS", [], [])
            
            return 0
            
        except Exception as e:
            self.logger.error(f"Failed to retrieve history: {e}")
            self._generate_execution_report("FAILED", [], [], str(e))
            raise
    
    def _execute_clean(self) -> int:
        """Clean database schemas (DANGER!)"""
        self.logger.warning("🚨 DANGER: This will clean all database schemas!")
        self.report_generator.display_warning_message(
            "This operation will permanently delete:\n"
            "  • All migration control tables\n"
            "  • All Flyway schema history\n"
            "  • All account tables and data\n"
            "  • All MongoDB collections\n"
        )
        
        # Require confirmation
        if not self._confirm_dangerous_operation():
            self.report_generator.display_info_message("✅ Clean operation cancelled")
            return 0
        
        try:
            # Connect to databases
            self.execution_control.connect()
            self.mongodb_engine.connect()
            
            # Clean PostgreSQL schemas
            self.logger.warning("💥 Cleaning PostgreSQL schemas...")
            cleaned = self.execution_control.clean_database_schemas(confirm=True)
            
            if not cleaned:
                self.report_generator.display_error_message("PostgreSQL cleanup failed")
                return 1
            
            # Clean MongoDB collections (best effort)
            try:
                self.logger.warning("💥 Cleaning MongoDB collections...")
                if self.mongodb_engine._database is not None:
                    # Drop all collections
                    collection_names = self.mongodb_engine._database.list_collection_names()
                    for collection_name in collection_names:
                        self.mongodb_engine._database.drop_collection(collection_name)
                        self.logger.info(f"Dropped collection: {collection_name}")
                    self.logger.info("✅ MongoDB collections cleaned")
            except Exception as mongo_error:
                self.logger.warning(f"MongoDB cleanup skipped: {mongo_error}")
                self.logger.info("💡 Tip: Ensure MongoDB is running without authentication for cleanup")
            
            self.report_generator.display_success_message("🎯 Database cleanup completed successfully!")
            self.logger.warning("All database schemas have been reset to initial state")
            
            return 0
            
        except Exception as e:
            error_msg = f"Clean operation failed: {e}"
            self.logger.error(error_msg)
            self.report_generator.display_error_message(error_msg)
            self._generate_execution_report("FAILED", [], [], error_msg)
            return 1
            raise
    
    def _generate_execution_report(self, status: str, 
                                 postgres_results: list = None,
                                 mongodb_results: list = None,
                                 error_message: str = None):
        """Generate comprehensive execution report"""
        if not self.start_time:
            return
            
        end_time = datetime.now(timezone.utc)
        
        # Convert results to dictionaries
        postgres_dict = []
        if postgres_results:
            for result in postgres_results:
                if hasattr(result, '__dict__'):
                    postgres_dict.append(result.__dict__)
                else:
                    postgres_dict.append(result)
        
        mongodb_dict = []
        if mongodb_results:
            for result in mongodb_results:
                if hasattr(result, '__dict__'):
                    mongodb_dict.append(result.__dict__)
                else:
                    mongodb_dict.append(result)
        
        # Generate JSON report
        report_path = self.report_generator.generate_execution_report(
            self.execution_id,
            "migrate",  # Default command for now
            self.environment,
            self.start_time,
            end_time,
            status,
            postgres_dict,
            mongodb_dict,
            error_message
        )
        
        # Generate console summary
        duration_seconds = int((end_time - self.start_time).total_seconds())
        self.report_generator.generate_console_summary(
            self.execution_id,
            "migrate",  # Default command for now
            self.environment,
            status,
            duration_seconds,
            postgres_dict,
            mongodb_dict,
            error_message,
            report_path
        )
    
    def _handle_execution_failure(self, error_message: str):
        """Handle execution failure"""
        if self.execution_id:
            try:
                self.execution_control.complete_execution(
                    self.execution_id, 
                    ExecutionStatus.FAILED,
                    error_message
                )
            except:
                pass  # Best effort
        
        self._generate_execution_report("FAILED", [], [], error_message)
    
    def _cleanup(self):
        """Cleanup database connections"""
        try:
            if hasattr(self, 'execution_control'):
                self.execution_control.disconnect()
            if hasattr(self, 'postgres_engine'):
                self.postgres_engine.disconnect()
            if hasattr(self, 'mongodb_engine'):
                self.mongodb_engine.disconnect()
        except:
            pass  # Best effort cleanup
    
    def _confirm_dangerous_operation(self) -> bool:
        """Confirm dangerous operations with user input"""
        try:
            response = input("Are you sure you want to proceed? Type 'YES' to confirm: ")
            return response.strip() == "YES"
        except (KeyboardInterrupt, EOFError):
            return False
    
    def _show_help(self):
        """Show help message"""
        print(__doc__)


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(
        description="Financer Database Migration Tool",
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    
    parser.add_argument("command", 
                       choices=["migrate", "info", "validate", "history", "clean"],
                       help="Migration command to execute")
    
    parser.add_argument("-e", "--environment", 
                       default="local",
                       help="Environment (local, docker, prod)")
    
    parser.add_argument("-p", "--profile",
                       help="Configuration profile")
    
    parser.add_argument("-v", "--verbose",
                       action="store_true",
                       help="Enable verbose logging")
    
    args = parser.parse_args()
    
    # Override environment with profile if provided
    environment = args.profile or args.environment
    
    try:
        runner = MigrationRunner(environment, args.verbose)
        exit_code = runner.run(args.command)
        sys.exit(exit_code)
        
    except Exception as e:
        print(f"❌ Fatal error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()