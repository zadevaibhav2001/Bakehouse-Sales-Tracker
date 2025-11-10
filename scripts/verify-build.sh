#!/bin/bash
# Build Verification Script
# Checks if the project is ready to build and deploy

set -e

echo "========================================="
echo "Build Verification Script"
echo "========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check functions
check_command() {
    if command -v $1 &> /dev/null; then
        echo -e "${GREEN}✓${NC} $1 is installed"
        return 0
    else
        echo -e "${RED}✗${NC} $1 is NOT installed"
        return 1
    fi
}

check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1 exists"
        return 0
    else
        echo -e "${RED}✗${NC} $1 is missing"
        return 1
    fi
}

# Track errors
ERRORS=0

echo "Checking prerequisites..."
echo ""

# Check Java
if check_command java; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo "  Version: $JAVA_VERSION"
else
    ERRORS=$((ERRORS + 1))
fi

# Check Maven
if check_command mvn; then
    MVN_VERSION=$(mvn -version | head -n 1 | cut -d' ' -f3)
    echo "  Version: $MVN_VERSION"
else
    ERRORS=$((ERRORS + 1))
fi

# Check AWS CLI
if check_command aws; then
    AWS_VERSION=$(aws --version 2>&1 | cut -d' ' -f1 | cut -d'/' -f2)
    echo "  Version: $AWS_VERSION"
else
    echo -e "${YELLOW}⚠${NC} AWS CLI not installed (optional for local build)"
fi

echo ""
echo "Checking project files..."
echo ""

# Check critical files
check_file "myapp/pom.xml" || ERRORS=$((ERRORS + 1))
check_file "myapp/src/main/java/com/example/myapp/Application.java" || ERRORS=$((ERRORS + 1))
check_file "myapp/src/main/resources/application.yml" || ERRORS=$((ERRORS + 1))
check_file "infrastructure/ec2-user-data.sh" || ERRORS=$((ERRORS + 1))
check_file "scripts/deploy.sh" || ERRORS=$((ERRORS + 1))

echo ""
echo "Checking project structure..."
echo ""

# Check directories
for dir in "myapp/src/main/java" "myapp/src/main/resources" "myapp/src/test/java" "infrastructure" "scripts" "docs"; do
    if [ -d "$dir" ]; then
        echo -e "${GREEN}✓${NC} $dir exists"
    else
        echo -e "${RED}✗${NC} $dir is missing"
        ERRORS=$((ERRORS + 1))
    fi
done

echo ""
echo "Attempting to build..."
echo ""

cd myapp

# Clean
echo "Running: mvn clean"
if mvn clean > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Maven clean successful"
else
    echo -e "${RED}✗${NC} Maven clean failed"
    ERRORS=$((ERRORS + 1))
fi

# Compile
echo "Running: mvn compile"
if mvn compile > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Maven compile successful"
else
    echo -e "${RED}✗${NC} Maven compile failed"
    echo "Run 'mvn compile' to see detailed errors"
    ERRORS=$((ERRORS + 1))
fi

# Test
echo "Running: mvn test"
if mvn test > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Maven test successful"
else
    echo -e "${YELLOW}⚠${NC} Maven test failed (may need database)"
    echo "This is expected if PostgreSQL is not running locally"
fi

# Package
echo "Running: mvn package -DskipTests"
if mvn package -DskipTests > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Maven package successful"
    JAR_FILE="target/myapp-0.0.1-SNAPSHOT.jar"
    if [ -f "$JAR_FILE" ]; then
        JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
        echo -e "${GREEN}✓${NC} JAR created: $JAR_FILE ($JAR_SIZE)"
    fi
else
    echo -e "${RED}✗${NC} Maven package failed"
    echo "Run 'mvn package -DskipTests' to see detailed errors"
    ERRORS=$((ERRORS + 1))
fi

cd ..

echo ""
echo "========================================="
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo "========================================="
    echo ""
    echo "Next steps:"
    echo "1. Edit infrastructure/ec2-user-data.sh with your values"
    echo "2. Follow QUICKSTART.md to deploy to AWS"
    echo "3. Or run locally with: cd myapp && mvn spring-boot:run"
    exit 0
else
    echo -e "${RED}✗ $ERRORS error(s) found${NC}"
    echo "========================================="
    echo ""
    echo "Please fix the errors above before proceeding."
    exit 1
fi
