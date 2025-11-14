#!/bin/bash

# Test script for the Workflow Example Application

BASE_URL="http://localhost:8080/api/orders"

echo "=== Workflow Example Application - Test Requests ==="
echo ""

# Health check
echo "1. Health Check"
echo "----------------------------------------"
curl -s $BASE_URL/health
echo -e "\n"

# Valid order
echo "2. Creating Valid Order"
echo "----------------------------------------"
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "amount": 99.99
  }' | python3 -m json.tool
echo -e "\n"

# Invalid order (zero amount)
echo "3. Creating Invalid Order (zero amount)"
echo "----------------------------------------"
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Jane Smith",
    "amount": 0
  }' | python3 -m json.tool
echo -e "\n"

# Large order
echo "4. Creating Large Order"
echo "----------------------------------------"
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Big Company Inc",
    "amount": 10000.00
  }' | python3 -m json.tool
echo -e "\n"

echo "=== Test Complete ==="
