#!/bin/bash

# Test script to verify CLI functionality in a single session

echo "=== Testing Scopes CLI ==="
echo ""

# Create parent scope
echo "1. Creating parent scope..."
./gradlew --quiet :boot:cli-launcher:run --args="create 'Parent Project' -d 'Main project scope'"

# Create child scope with valid parent ID
echo ""
echo "2. Creating child scope with ULID parent..."
./gradlew --quiet :boot:cli-launcher:run --args="create 'Feature Development' -d 'Feature work' -p '01K35Q28W04M3E0B8F2CYW1XDX'"

# Test error cases
echo ""
echo "3. Testing invalid parent ID..."
./gradlew --quiet :boot:cli-launcher:run --args="create 'Invalid Child' -p 'not-a-ulid'"

echo ""
echo "4. Testing get command..."
./gradlew --quiet :boot:cli-launcher:run --args="get '01K35Q28W04M3E0B8F2CYW1XDX'"

echo ""
echo "=== Test Complete ==="
