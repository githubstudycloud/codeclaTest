#!/bin/bash

# High Performance Cache Middleware Build Script
# Supports building for different Spring Boot versions

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
SPRING_BOOT_VERSION="3x"
SKIP_TESTS=false
CLEAN_BUILD=false

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -v, --version VERSION    Spring Boot version (1x, 2x, 3x) [default: 3x]"
    echo "  -s, --skip-tests        Skip running tests"
    echo "  -c, --clean             Clean before build"
    echo "  -h, --help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                      # Build with Spring Boot 3.x"
    echo "  $0 -v 2x -s            # Build with Spring Boot 2.x, skip tests"
    echo "  $0 -c                   # Clean build with Spring Boot 3.x"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--version)
            SPRING_BOOT_VERSION="$2"
            shift 2
            ;;
        -s|--skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        -c|--clean)
            CLEAN_BUILD=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate Spring Boot version
if [[ ! "$SPRING_BOOT_VERSION" =~ ^(1x|2x|3x)$ ]]; then
    print_error "Invalid Spring Boot version: $SPRING_BOOT_VERSION"
    print_error "Supported versions: 1x, 2x, 3x"
    exit 1
fi

print_info "Building High Performance Cache Middleware"
print_info "Spring Boot Version: $SPRING_BOOT_VERSION"
print_info "Skip Tests: $SKIP_TESTS"
print_info "Clean Build: $CLEAN_BUILD"

# Set Maven profile based on Spring Boot version
MAVEN_PROFILE=""
case $SPRING_BOOT_VERSION in
    1x)
        MAVEN_PROFILE="-Pspring-boot-1x"
        ;;
    2x)
        MAVEN_PROFILE="-Pspring-boot-2x"
        ;;
    3x)
        MAVEN_PROFILE=""  # Default profile for 3.x
        ;;
esac

# Build Maven command
MAVEN_CMD="./mvnw"
if [[ "$CLEAN_BUILD" == true ]]; then
    MAVEN_CMD="$MAVEN_CMD clean"
fi

MAVEN_CMD="$MAVEN_CMD package"

if [[ "$SKIP_TESTS" == true ]]; then
    MAVEN_CMD="$MAVEN_CMD -DskipTests"
fi

if [[ -n "$MAVEN_PROFILE" ]]; then
    MAVEN_CMD="$MAVEN_CMD $MAVEN_PROFILE"
fi

# Check if Maven wrapper exists
if [[ ! -f "./mvnw" ]]; then
    print_error "Maven wrapper not found. Please run: mvn wrapper:wrapper"
    exit 1
fi

# Execute build
print_info "Executing: $MAVEN_CMD"
eval $MAVEN_CMD

if [[ $? -eq 0 ]]; then
    print_info "Build completed successfully!"
    print_info "Artifacts location:"
    echo "  - Server: cache-server/cache-server-core/target/"
    echo "  - Client: cache-client-java/cache-client-spring-boot-starter-${SPRING_BOOT_VERSION}/target/"
else
    print_error "Build failed!"
    exit 1
fi