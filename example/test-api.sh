#!/bin/bash

# Color codes for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Workflow Example Application - API Test Script ===${NC}\n"

BASE_URL="http://localhost:8080/api/orders"

# Test 1: Submit Standard Shipping Order
echo -e "${GREEN}Test 1: Submitting order with STANDARD shipping${NC}"
curl -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d @sample-requests/create-order-standard.json \
  -w "\n\n"

sleep 1

# Test 2: Submit Express Shipping Order
echo -e "${GREEN}Test 2: Submitting order with EXPRESS shipping${NC}"
curl -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d @sample-requests/create-order-express.json \
  -w "\n\n"

sleep 1

# Test 3: Submit Overnight Shipping Order
echo -e "${GREEN}Test 3: Submitting order with OVERNIGHT shipping${NC}"
curl -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d @sample-requests/create-order-overnight.json \
  -w "\n\n"

sleep 1

# Test 4: Get Order Status
echo -e "${GREEN}Test 4: Getting order status for ORD-2025-001${NC}"
curl -X GET $BASE_URL/ORD-2025-001 \
  -H "Content-Type: application/json" \
  -w "\n\n"

sleep 1

# Test 5: Resume Workflow (example)
echo -e "${GREEN}Test 5: Resuming workflow for ORD-2025-001${NC}"
curl -X POST $BASE_URL/ORD-2025-001/resume \
  -H "Content-Type: application/json" \
  -w "\n\n"

echo -e "${BLUE}=== Tests Complete ===${NC}"
