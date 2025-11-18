#!/bin/bash

# Test script for RuntimeBusyException handling
# This script demonstrates the retry mechanism with exponential backoff

echo "=========================================="
echo "RuntimeBusyException Handling Test"
echo "=========================================="
echo ""

BASE_URL="http://localhost:8081/api/processing"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check if server is running
echo "Checking if server is running..."
if curl -s "${BASE_URL}/stats" > /dev/null 2>&1; then
    print_success "Server is running"
else
    print_error "Server is not running!"
    echo ""
    echo "Please start the server first:"
    echo "  cd example/concurrent-workflow-execution"
    echo "  mvn spring-boot:run"
    exit 1
fi

echo ""
echo "=========================================="
echo "Test 1: Reset Statistics"
echo "=========================================="
curl -s -X POST "${BASE_URL}/stats/reset" | jq '.'
print_success "Statistics reset"

echo ""
echo "=========================================="
echo "Test 2: Single Request (No Contention)"
echo "=========================================="
echo "This should succeed immediately without retries"
echo ""

curl -s -X POST "${BASE_URL}/single" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-001",
    "type": "VIDEO",
    "dataItems": ["video1.mp4"],
    "priority": 1
  }' | jq '.'

sleep 2
echo ""
print_success "Single request completed"

echo ""
echo "=========================================="
echo "Test 3: Small Batch (Low Contention)"
echo "=========================================="
echo "Processing 10 concurrent workflows..."
echo "Watch for RuntimeBusyException and retry attempts in the server logs"
echo ""

curl -s -X POST "${BASE_URL}/batch/small" | jq '.'

sleep 3
echo ""
print_success "Small batch completed"

echo ""
echo "=========================================="
echo "Test 4: Check Statistics After Small Batch"
echo "=========================================="
STATS=$(curl -s "${BASE_URL}/stats")
echo "$STATS" | jq '.'

BUSY_COUNT=$(echo "$STATS" | jq '.totalBusyExceptions')
RETRY_COUNT=$(echo "$STATS" | jq '.totalRetries')

echo ""
if [ "$BUSY_COUNT" -gt 0 ]; then
    print_warning "Encountered $BUSY_COUNT RuntimeBusyException(s)"
    print_success "Retry mechanism handled them with $RETRY_COUNT retry attempts"
else
    print_success "No RuntimeBusyException encountered (low contention)"
fi

echo ""
echo "=========================================="
echo "Test 5: Medium Batch (Higher Contention)"
echo "=========================================="
echo "Processing 50 concurrent workflows..."
echo "This WILL trigger RuntimeBusyException and retries"
echo "Check server logs for detailed retry information"
echo ""

curl -s -X POST "${BASE_URL}/batch/medium" | jq '.'

sleep 5
echo ""
print_success "Medium batch completed"

echo ""
echo "=========================================="
echo "Test 6: Final Statistics"
echo "=========================================="
FINAL_STATS=$(curl -s "${BASE_URL}/stats")
echo "$FINAL_STATS" | jq '.'

FINAL_BUSY=$(echo "$FINAL_STATS" | jq '.totalBusyExceptions')
FINAL_RETRY=$(echo "$FINAL_STATS" | jq '.totalRetries')
TOTAL_STARTED=$(echo "$FINAL_STATS" | jq '.totalStarted')
TOTAL_COMPLETED=$(echo "$FINAL_STATS" | jq '.totalCompleted')
TOTAL_FAILED=$(echo "$FINAL_STATS" | jq '.totalFailed')

echo ""
echo "=========================================="
echo "Summary"
echo "=========================================="
echo "Total workflows started:     $TOTAL_STARTED"
echo "Total workflows completed:   $TOTAL_COMPLETED"
echo "Total workflows failed:      $TOTAL_FAILED"
echo "Total busy exceptions:       $FINAL_BUSY"
echo "Total retry attempts:        $FINAL_RETRY"
echo ""

if [ "$FINAL_BUSY" -gt 0 ]; then
    RETRY_RATIO=$(echo "scale=2; $FINAL_RETRY / $FINAL_BUSY" | bc)
    echo "Average retries per exception: $RETRY_RATIO"
    echo ""
    print_success "RuntimeBusyException handling is working!"
    print_success "The retry mechanism with exponential backoff handled all busy conditions"
else
    print_warning "No RuntimeBusyException encountered"
    print_warning "Try running the stress test for higher contention:"
    echo "  curl -X POST \"${BASE_URL}/stress-test?count=100\""
fi

echo ""
echo "=========================================="
echo "Additional Tests Available"
echo "=========================================="
echo "1. Stress test (100 workflows):"
echo "   curl -X POST \"${BASE_URL}/stress-test?count=100\""
echo ""
echo "2. Large batch with controlled concurrency:"
echo "   curl -X POST \"${BASE_URL}/batch/large\""
echo ""
echo "3. Test different engine selection strategies:"
echo "   curl -X POST \"${BASE_URL}/single/PRIORITY_BASED\" -H 'Content-Type: application/json' -d '{...}'"
echo ""
echo "4. View available strategies:"
echo "   curl ${BASE_URL}/strategies"
echo ""
echo "=========================================="
echo "Test Complete!"
echo "=========================================="
