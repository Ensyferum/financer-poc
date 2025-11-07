#!/bin/bash

# Financer Development Environment Setup Script
# This script sets up the development environment and installs Git hooks

set -e

echo "🚀 Setting up Financer development environment..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_color() {
    printf "${1}${2}${NC}\n"
}

print_success() {
    print_color $GREEN "✅ $1"
}

print_error() {
    print_color $RED "❌ $1"
}

print_warning() {
    print_color $YELLOW "⚠️ $1"
}

print_info() {
    print_color $BLUE "ℹ️ $1"
}

# Check if Git is installed
check_git() {
    if ! command -v git &> /dev/null; then
        print_error "Git is not installed. Please install Git first."
        exit 1
    fi
    print_success "Git is installed"
}

# Check if Java is installed
check_java() {
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 21 or higher."
        exit 1
    fi
    
    java_version=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 21 ]; then
        print_error "Java 21 or higher is required. Found version: $java_version"
        exit 1
    fi
    print_success "Java $java_version is installed"
}

# Check if Maven is installed
check_maven() {
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven 3.9 or higher."
        exit 1
    fi
    
    maven_version=$(mvn -version 2>&1 | head -n1 | grep -oP 'Apache Maven \K[0-9]+\.[0-9]+')
    print_success "Maven $maven_version is installed"
}

# Check if Docker is installed
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker."
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker is not running. Please start Docker."
        exit 1
    fi
    print_success "Docker is installed and running"
}

# Check if Docker Compose is installed
check_docker_compose() {
    if ! command -v docker-compose &> /dev/null; then
        print_warning "Docker Compose V1 not found, checking for Docker Compose V2..."
        if ! docker compose version &> /dev/null; then
            print_error "Docker Compose is not installed. Please install Docker Compose."
            exit 1
        fi
        print_success "Docker Compose V2 is installed"
    else
        print_success "Docker Compose V1 is installed"
    fi
}

# Install Git hooks
install_git_hooks() {
    print_info "Installing Git hooks..."
    
    # Create hooks directory if it doesn't exist
    mkdir -p .git/hooks
    
    # Pre-commit hook
    cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
# Pre-commit hook for Financer project

echo "🔍 Running pre-commit checks..."

# Check for secrets
if git diff --cached --name-only | xargs grep -l "password\|secret\|key\|token" 2>/dev/null; then
    echo "❌ Potential secrets found in staged files. Please remove them."
    exit 1
fi

# Run Maven compile
echo "🔨 Compiling project..."
if ! mvn clean compile -q; then
    echo "❌ Compilation failed. Please fix compilation errors."
    exit 1
fi

# Run tests
echo "🧪 Running tests..."
if ! mvn test -q; then
    echo "❌ Tests failed. Please fix failing tests."
    exit 1
fi

echo "✅ Pre-commit checks passed!"
EOF

    # Commit message hook
    cat > .git/hooks/commit-msg << 'EOF'
#!/bin/bash
# Commit message hook for conventional commits

commit_regex='^(feat|fix|docs|style|refactor|test|chore|ci|perf|build)(\(.+\))?: .{1,50}'

if ! grep -qE "$commit_regex" "$1"; then
    echo "❌ Invalid commit message format!"
    echo "Please use conventional commit format:"
    echo "  feat: add new feature"
    echo "  fix: fix bug"
    echo "  docs: update documentation"
    echo "  style: format code"
    echo "  refactor: refactor code"
    echo "  test: add tests"
    echo "  chore: maintenance"
    echo "  ci: update CI/CD"
    echo "  perf: performance improvement"
    echo "  build: build system changes"
    exit 1
fi
EOF

    # Make hooks executable
    chmod +x .git/hooks/pre-commit
    chmod +x .git/hooks/commit-msg
    
    print_success "Git hooks installed"
}

# Setup Maven wrapper if not exists
setup_maven_wrapper() {
    if [ ! -f "mvnw" ]; then
        print_info "Setting up Maven wrapper..."
        mvn wrapper:wrapper
        print_success "Maven wrapper setup complete"
    else
        print_success "Maven wrapper already exists"
    fi
}

# Build project
build_project() {
    print_info "Building project..."
    
    # Install parent POM
    mvn clean install -N -q
    
    # Build shared libraries
    cd shared/common-lib && mvn clean install -q && cd ../..
    cd shared/eureka-integration && mvn clean install -q && cd ../..
    
    # Build microservices
    for service in config-server eureka-server api-gateway account-service; do
        print_info "Building $service..."
        cd microservices/$service && mvn clean package -DskipTests -q && cd ../..
    done
    
    print_success "Project build complete"
}

# Create development configuration
create_dev_config() {
    print_info "Creating development configuration..."
    
    if [ ! -f ".env.development" ]; then
        cat > .env.development << 'EOF'
# Development Environment Configuration
SPRING_PROFILES_ACTIVE=development
LOG_LEVEL=DEBUG
DATABASE_URL=jdbc:postgresql://localhost:5432/financer
DATABASE_USERNAME=financer
DATABASE_PASSWORD=financer123
MONGODB_URL=mongodb://financer:financer123@localhost:27017/financer
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
EUREKA_SERVER_URL=http://localhost:8761/eureka
CONFIG_SERVER_URL=http://localhost:8888
EOF
        print_success "Development configuration created"
    else
        print_success "Development configuration already exists"
    fi
}

# Setup IDE configuration
setup_ide_config() {
    print_info "Setting up IDE configuration..."
    
    # IntelliJ IDEA configuration
    if [ ! -d ".idea" ]; then
        mkdir -p .idea
        
        cat > .idea/compiler.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="CompilerConfiguration">
    <annotationProcessing>
      <profile name="Maven default annotation processors profile" enabled="true">
        <sourceOutputDir name="target/generated-sources/annotations" />
        <sourceTestOutputDir name="target/generated-test-sources/test-annotations" />
        <outputRelativeToContentRoot value="true" />
      </profile>
    </annotationProcessing>
  </component>
</project>
EOF
    fi
    
    # VS Code configuration
    if [ ! -d ".vscode" ]; then
        mkdir -p .vscode
        
        cat > .vscode/settings.json << 'EOF'
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "java.compile.nullAnalysis.mode": "automatic",
    "maven.terminal.useJavaHome": true,
    "files.exclude": {
        "**/target": true,
        "**/.classpath": true,
        "**/.project": true,
        "**/.settings": true,
        "**/.factorypath": true
    }
}
EOF

        cat > .vscode/extensions.json << 'EOF'
{
    "recommendations": [
        "vscjava.vscode-java-pack",
        "vscjava.vscode-spring-boot-dashboard",
        "ms-vscode.vscode-docker",
        "github.vscode-github-actions"
    ]
}
EOF
    fi
    
    print_success "IDE configuration setup complete"
}

# Print next steps
print_next_steps() {
    print_color $BLUE "\n🎉 Development environment setup complete!\n"
    print_info "Next steps:"
    echo "  1. Start infrastructure: ./scripts/start-infrastructure.bat"
    echo "  2. Build and start services: ./scripts/start-services.bat"
    echo "  3. Or start everything: ./scripts/start-all.bat"
    echo ""
    print_info "Useful URLs:"
    echo "  • Eureka Dashboard: http://localhost:8761"
    echo "  • API Gateway: http://localhost:8090"
    echo "  • Config Server: http://localhost:8888"
    echo "  • Kafka UI: http://localhost:8080"
    echo ""
    print_info "Development commands:"
    echo "  • Run tests: mvn test"
    echo "  • Build project: mvn clean package"
    echo "  • Start single service: cd microservices/<service> && mvn spring-boot:run"
    echo ""
    print_warning "Remember to follow conventional commit messages!"
    print_warning "Pre-commit hooks are now active."
}

# Main execution
main() {
    print_color $BLUE "🔧 Financer Development Environment Setup"
    echo ""
    
    print_info "Checking prerequisites..."
    check_git
    check_java
    check_maven
    check_docker
    check_docker_compose
    
    print_info "Setting up development environment..."
    install_git_hooks
    setup_maven_wrapper
    create_dev_config
    setup_ide_config
    
    print_info "Building project..."
    build_project
    
    print_next_steps
}

# Run main function
main "$@"