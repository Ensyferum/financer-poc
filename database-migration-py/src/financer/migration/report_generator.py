"""
Financer Database Migration System - Report Generator
=====================================================

Comprehensive reporting system for migration executions with JSON and console output.
"""

import json
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Any, List, Optional
from dataclasses import dataclass, asdict
import platform
import getpass
from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.text import Text


@dataclass
class SystemInfo:
    """System information for reports"""
    python_version: str
    os_name: str
    os_version: str
    user_home: str
    working_directory: str
    hostname: str


@dataclass
class MigrationReport:
    """Complete migration execution report"""
    execution_id: str
    command: str
    environment: str
    start_time: str
    end_time: str
    status: str
    duration_seconds: int
    error_message: Optional[str]
    postgres_results: List[Dict[str, Any]]
    mongodb_results: List[Dict[str, Any]]
    executed_by: str
    generated_at: str
    system_info: SystemInfo


class ReportGenerator:
    """
    Generates detailed migration reports in JSON format and beautiful console summaries.
    Maintains compatibility with the original Spring Boot reporting system.
    """
    
    def __init__(self, logger):
        self.logger = logger
        self.console = Console()
        self.reports_dir = Path("logs/migration-reports")
        self.reports_dir.mkdir(parents=True, exist_ok=True)
    
    def generate_execution_report(self, 
                                execution_id: str,
                                command: str,
                                environment: str,
                                start_time: datetime,
                                end_time: datetime,
                                status: str,
                                postgres_results: List[Dict[str, Any]] = None,
                                mongodb_results: List[Dict[str, Any]] = None,
                                error_message: str = None) -> str:
        """Generate complete execution report and save to JSON file"""
        
        duration_seconds = int((end_time - start_time).total_seconds())
        
        # Create system info
        system_info = SystemInfo(
            python_version=platform.python_version(),
            os_name=platform.system(),
            os_version=platform.version(),
            user_home=str(Path.home()),
            working_directory=str(Path.cwd()),
            hostname=platform.node()
        )
        
        # Create report
        report = MigrationReport(
            execution_id=execution_id,
            command=command,
            environment=environment,
            start_time=start_time.isoformat(),
            end_time=end_time.isoformat(),
            status=status,
            duration_seconds=duration_seconds,
            error_message=error_message,
            postgres_results=postgres_results or [],
            mongodb_results=mongodb_results or [],
            executed_by=self._get_executed_by(),
            generated_at=datetime.now(timezone.utc).isoformat(),
            system_info=system_info
        )
        
        # Save to JSON file
        report_filename = f"migration-report-{start_time.strftime('%Y%m%d-%H%M%S')}-{execution_id}.json"
        report_path = self.reports_dir / report_filename
        
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(asdict(report), f, indent=2, ensure_ascii=False)
        
        self.logger.log_report_generation(str(report_path))
        
        return str(report_path)
    
    def generate_console_summary(self,
                                execution_id: str,
                                command: str,
                                environment: str,
                                status: str,
                                duration_seconds: int,
                                postgres_results: List[Dict[str, Any]] = None,
                                mongodb_results: List[Dict[str, Any]] = None,
                                error_message: str = None,
                                report_path: str = None):
        """Generate beautiful console summary report"""
        
        # Prepare status emoji and color
        if status == "SUCCESS":
            status_display = "✅ SUCCESS"
            status_color = "green"
        elif status == "FAILED":
            status_display = "❌ FAILED"
            status_color = "red"
        else:
            status_display = f"⚠️ {status}"
            status_color = "yellow"
        
        # Create main panel content
        panel_content = []
        
        # Header
        panel_content.append(f"Execution ID: {execution_id[:36]}")
        panel_content.append(f"Command: {command}")
        panel_content.append(f"Environment: {environment}")
        panel_content.append(f"Status: {status_display}")
        panel_content.append(f"Duration: {duration_seconds} seconds")
        panel_content.append(f"Executed by: {self._get_executed_by()}")
        
        # Separator
        panel_content.append("═" * 60)
        
        # PostgreSQL Results
        if postgres_results:
            successful_postgres = len([r for r in postgres_results if r.get('successful', True)])
            failed_postgres = len(postgres_results) - successful_postgres
            
            panel_content.append(f"PostgreSQL Migrations: {len(postgres_results)} executed")
            panel_content.append(f"├─ Successful: {successful_postgres}")
            panel_content.append(f"└─ Failed: {failed_postgres}")
        
        # MongoDB Results
        if mongodb_results:
            successful_mongodb = len([r for r in mongodb_results if r.get('successful', True)])
            failed_mongodb = len(mongodb_results) - successful_mongodb
            
            panel_content.append(f"MongoDB Migrations: {len(mongodb_results)} executed")
            panel_content.append(f"├─ Successful: {successful_mongodb}")
            panel_content.append(f"└─ Failed: {failed_mongodb}")
        
        # Error details
        if error_message:
            panel_content.append("═" * 60)
            panel_content.append("ERROR DETAILS:")
            panel_content.append(f"  {error_message}")
        
        # Report file info
        if report_path:
            panel_content.append("═" * 60)
            filename = Path(report_path).name
            panel_content.append(f"Report saved to: ...{filename}")
        
        # Create and display panel
        panel = Panel(
            "\n".join(panel_content),
            title="MIGRATION EXECUTION REPORT",
            title_align="center",
            border_style=status_color,
            padding=(1, 2)
        )
        
        self.console.print()
        self.console.print(panel)
        self.console.print()
    
    def display_migration_info_table(self, 
                                   postgres_info: List[Dict[str, Any]] = None,
                                   mongodb_info: List[Dict[str, Any]] = None):
        """Display migration information in formatted tables"""
        
        # PostgreSQL Migrations Table
        if postgres_info:
            postgres_table = Table(title="PostgreSQL Migrations", show_header=True, header_style="bold magenta")
            postgres_table.add_column("Status", style="cyan", width=12)
            postgres_table.add_column("Version", style="yellow", width=15)
            postgres_table.add_column("Type", style="green", width=15)
            postgres_table.add_column("Description", style="white", width=20)
            
            for migration in postgres_info:
                status = migration.get('status', 'Unknown')
                version = migration.get('version', 'N/A')
                migration_type = migration.get('type', 'SQL')
                description = migration.get('description', 'N/A')
                
                # Truncate long descriptions
                if len(description) > 20:
                    description = description[:17] + "..."
                
                # Color code status
                if status == "Success":
                    status_colored = Text(status, style="green")
                elif status == "Failed":
                    status_colored = Text(status, style="red")
                elif status == "Pending":
                    status_colored = Text(status, style="yellow")
                else:
                    status_colored = Text(status, style="white")
                
                postgres_table.add_row(status_colored, version, migration_type, description)
            
            self.console.print(postgres_table)
            self.console.print()
        
        # MongoDB Migrations Table
        if mongodb_info:
            mongodb_table = Table(title="MongoDB Migrations", show_header=True, header_style="bold green")
            mongodb_table.add_column("Status", style="cyan", width=12)
            mongodb_table.add_column("Version", style="yellow", width=15)
            mongodb_table.add_column("Type", style="green", width=15)
            mongodb_table.add_column("Description", style="white", width=20)
            mongodb_table.add_column("Operations", style="blue", width=10)
            
            for migration in mongodb_info:
                status = migration.get('status', 'Unknown')
                version = migration.get('version', 'N/A')
                migration_type = migration.get('type', 'NoSQL')
                description = migration.get('description', 'N/A')
                operations = str(migration.get('operations_count', 0))
                
                # Truncate long descriptions
                if len(description) > 20:
                    description = description[:17] + "..."
                
                # Color code status
                if status == "Success":
                    status_colored = Text(status, style="green")
                elif status == "Failed":
                    status_colored = Text(status, style="red")
                elif status == "Pending":
                    status_colored = Text(status, style="yellow")
                else:
                    status_colored = Text(status, style="white")
                
                mongodb_table.add_row(status_colored, version, migration_type, description, operations)
            
            self.console.print(mongodb_table)
            self.console.print()
    
    def display_execution_history(self, history: List[Dict[str, Any]], limit: int = 10):
        """Display execution history in a formatted table"""
        
        if not history:
            self.console.print("[yellow]No migration execution history found.[/yellow]")
            return
        
        history_table = Table(title=f"Migration Execution History (Last {min(len(history), limit)})", 
                             show_header=True, header_style="bold cyan")
        
        history_table.add_column("ID", style="yellow", width=8)
        history_table.add_column("Command", style="green", width=10)
        history_table.add_column("Environment", style="blue", width=12)
        history_table.add_column("Status", style="cyan", width=10)
        history_table.add_column("Duration", style="magenta", width=10)
        history_table.add_column("Executed At", style="white", width=16)
        history_table.add_column("By", style="white", width=12)
        
        for execution in history[:limit]:
            execution_id = execution.get('execution_id', 'N/A')[:8]
            command = execution.get('command', 'N/A')
            environment = execution.get('environment', 'N/A')
            status = execution.get('status', 'N/A')
            duration = f"{execution.get('duration_seconds', 0)}s"
            executed_by = execution.get('executed_by', 'N/A')
            
            # Format start time
            start_time = execution.get('start_time', '')
            if start_time:
                try:
                    dt = datetime.fromisoformat(start_time.replace('Z', '+00:00'))
                    formatted_time = dt.strftime('%Y-%m-%d %H:%M')
                except:
                    formatted_time = start_time[:16]
            else:
                formatted_time = 'N/A'
            
            # Color code status
            if status == "SUCCESS":
                status_colored = Text(status, style="green")
            elif status == "FAILED":
                status_colored = Text(status, style="red")
            else:
                status_colored = Text(status, style="yellow")
            
            # Truncate executed_by if too long
            if len(executed_by) > 12:
                executed_by = executed_by[:9] + "..."
            
            history_table.add_row(
                execution_id, command, environment, status_colored, 
                duration, formatted_time, executed_by
            )
        
        self.console.print(history_table)
        self.console.print()
    
    def display_welcome_message(self):
        """Display welcome message"""
        welcome_text = Text("🚀 Financer Database Migration Tool v2.0.0 (Python)", style="bold green")
        self.console.print()
        self.console.print(welcome_text)
        self.console.print("=" * 60)
    
    def display_success_message(self, message: str):
        """Display success message"""
        self.console.print(f"[green]✅ {message}[/green]")
    
    def display_error_message(self, message: str):
        """Display error message"""
        self.console.print(f"[red]❌ {message}[/red]")
    
    def display_info_message(self, message: str):
        """Display info message"""
        self.console.print(f"[blue]ℹ️ {message}[/blue]")
    
    def display_warning_message(self, message: str):
        """Display warning message"""
        self.console.print(f"[yellow]⚠️ {message}[/yellow]")
    
    def _get_executed_by(self) -> str:
        """Get current user information"""
        try:
            username = getpass.getuser()
            hostname = platform.node()
            return f"{username}@{hostname}"
        except:
            return "system"