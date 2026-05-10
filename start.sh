#!/bin/bash

# Finance Tracker Startup Script
# This script starts the Spring Boot Finance Tracker application

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if .env file exists
check_env_file() {
    if [ ! -f ".env" ]; then
        print_warning ".env file not found!"
        echo ""
        print_status "Creating .env file from .env.example..."
        
        if [ -f ".env.example" ]; then
            cp .env.example .env
            print_success ".env file created from .env.example"
            print_warning "Please edit .env file with your MongoDB credentials before running the application again!"
            echo ""
            print_status "To configure MongoDB:"
            echo "1. Go to MongoDB Atlas (https://cloud.mongodb.com)"
            echo "2. Create a free cluster or use existing one"
            echo "3. Create a database user with read/write permissions"
            echo "4. Get the connection string from Connect -> Drivers"
            echo "5. Edit .env file and replace 'your-username' and 'your-password' with actual credentials"
            echo ""
            exit 1
        else
            print_error ".env.example file not found!"
            exit 1
        fi
    fi
}

# Check Java version
check_java() {
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 17 ]; then
            print_success "Java $JAVA_VERSION found (required: Java 17+)"
        else
            print_error "Java $JAVA_VERSION found but Java 17+ is required!"
            print_status "Please install Java 17 or higher"
            exit 1
        fi
    else
        print_error "Java not found!"
        print_status "Please install Java 17 or higher"
        exit 1
    fi
}

# Check Maven wrapper
check_maven_wrapper() {
    if [ -f "./mvnw" ]; then
        print_success "Maven wrapper found"
        chmod +x mvnw
    else
        print_error "Maven wrapper (mvnw) not found!"
        exit 1
    fi
}

# Check if port is available
check_port() {
    PORT=${SERVER_PORT:-8080}
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        print_warning "Port $PORT is already in use!"
        print_status "Trying to kill existing process on port $PORT..."
        lsof -ti:$PORT | xargs kill -9 2>/dev/null || true
        sleep 2
        if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
            print_error "Could not free port $PORT. Please manually stop the process using it."
            exit 1
        else
            print_success "Port $PORT is now available"
        fi
    else
        print_success "Port $PORT is available"
    fi
}

# Load environment variables
load_env() {
    if [ -f ".env" ]; then
        set -a
        source .env
        set +a
        print_success "Environment variables loaded from .env"
    fi
}

# Start the application
start_application() {
    echo ""
    print_status "Starting Finance Tracker Application..."
    echo ""
    
    # Load environment variables
    load_env
    
    # Display configuration
    print_status "Configuration:"
    echo "  - MongoDB Database: ${MONGODB_DATABASE:-finance-tracker}"
    echo "  - Server Port: ${SERVER_PORT:-8080}"
    echo ""
    
    # Start the application
    print_status "Running: ./mvnw spring-boot:run"
    echo ""
    ./mvnw spring-boot:run
}

# Handle script interruption
cleanup() {
    print_status "Shutting down Finance Tracker..."
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

# Main execution
main() {
    echo ""
    echo "========================================"
    echo "    Finance Tracker Startup Script      "
    echo "========================================"
    echo ""
    
    # Run checks
    check_java
    check_maven_wrapper
    check_env_file
    check_port
    
    # Start the application
    start_application
}

# Run main function
main "$@"
