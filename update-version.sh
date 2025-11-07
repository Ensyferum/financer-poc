#!/bin/bash

# Financer Version Management Script
# Usage: ./update-version.sh [component] [new-version] [version-type]
# Example: ./update-version.sh account-service 1.0.1 patch

COMPONENT=$1
NEW_VERSION=$2
VERSION_TYPE=$3

if [ -z "$COMPONENT" ] || [ -z "$NEW_VERSION" ] || [ -z "$VERSION_TYPE" ]; then
    echo "Usage: $0 [component] [new-version] [version-type]"
    echo "Components: config-server, eureka-server, api-gateway, account-service, transaction-service, orchestration-service, common-lib, all"
    echo "Version types: major, minor, patch"
    exit 1
fi

# Function to validate semantic version
validate_version() {
    if [[ ! $1 =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo "Error: Invalid version format. Use MAJOR.MINOR.PATCH (e.g., 1.0.0)"
        exit 1
    fi
}

# Function to update version in VERSION.properties
update_version_properties() {
    local component=$1
    local version=$2
    
    case $component in
        "config-server")
            sed -i "s/CONFIG_SERVER_VERSION=.*/CONFIG_SERVER_VERSION=$version/" VERSION.properties
            ;;
        "eureka-server")
            sed -i "s/EUREKA_SERVER_VERSION=.*/EUREKA_SERVER_VERSION=$version/" VERSION.properties
            ;;
        "api-gateway")
            sed -i "s/API_GATEWAY_VERSION=.*/API_GATEWAY_VERSION=$version/" VERSION.properties
            ;;
        "account-service")
            sed -i "s/ACCOUNT_SERVICE_VERSION=.*/ACCOUNT_SERVICE_VERSION=$version/" VERSION.properties
            ;;
        "transaction-service")
            sed -i "s/TRANSACTION_SERVICE_VERSION=.*/TRANSACTION_SERVICE_VERSION=$version/" VERSION.properties
            ;;
        "orchestration-service")
            sed -i "s/ORCHESTRATION_SERVICE_VERSION=.*/ORCHESTRATION_SERVICE_VERSION=$version/" VERSION.properties
            ;;
        "common-lib")
            sed -i "s/COMMON_LIB_VERSION=.*/COMMON_LIB_VERSION=$version/" VERSION.properties
            ;;
    esac
}

# Function to update version in pom.xml
update_pom_version() {
    local component=$1
    local version=$2
    
    case $component in
        "config-server")
            sed -i "s/<config-server.version>.*<\/config-server.version>/<config-server.version>$version<\/config-server.version>/" pom.xml
            ;;
        "eureka-server")
            sed -i "s/<eureka-server.version>.*<\/eureka-server.version>/<eureka-server.version>$version<\/eureka-server.version>/" pom.xml
            ;;
        "api-gateway")
            sed -i "s/<api-gateway.version>.*<\/api-gateway.version>/<api-gateway.version>$version<\/api-gateway.version>/" pom.xml
            ;;
        "account-service")
            sed -i "s/<account-service.version>.*<\/account-service.version>/<account-service.version>$version<\/account-service.version>/" pom.xml
            ;;
        "transaction-service")
            sed -i "s/<transaction-service.version>.*<\/transaction-service.version>/<transaction-service.version>$version<\/transaction-service.version>/" pom.xml
            ;;
        "orchestration-service")
            sed -i "s/<orchestration-service.version>.*<\/orchestration-service.version>/<orchestration-service.version>$version<\/orchestration-service.version>/" pom.xml
            ;;
        "common-lib")
            sed -i "s/<common-lib.version>.*<\/common-lib.version>/<common-lib.version>$version<\/common-lib.version>/" pom.xml
            ;;
    esac
}

# Validate version format
validate_version $NEW_VERSION

echo "Updating $COMPONENT to version $NEW_VERSION ($VERSION_TYPE update)"

if [ "$COMPONENT" = "all" ]; then
    echo "Error: Mass version update not implemented yet. Update components individually."
    exit 1
fi

# Update version files
update_version_properties $COMPONENT $NEW_VERSION
update_pom_version $COMPONENT $NEW_VERSION

# Add changelog entry
echo "# $NEW_VERSION - $(date '+%Y-%m-%d') - $VERSION_TYPE update" >> CHANGELOG.md
echo "#   - $COMPONENT: Updated to $NEW_VERSION" >> CHANGELOG.md
echo "" >> CHANGELOG.md

echo "Successfully updated $COMPONENT to version $NEW_VERSION"
echo "Don't forget to:"
echo "1. Update the changelog with specific changes"
echo "2. Rebuild the affected services"
echo "3. Tag the release in git: git tag $COMPONENT-$NEW_VERSION"
echo "4. Push changes: git push && git push --tags"