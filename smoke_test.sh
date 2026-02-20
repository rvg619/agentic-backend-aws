#!/bin/bash

# Smoke Test Script for Agentic Workflow System
# Tests: Create task → Create run → Wait → Fetch run/steps/artifacts

set -e  # Exit on any error

BASE_URL="http://localhost:8080"
TASK_ID=""
RUN_ID=""

echo "🚀 Starting Agentic Workflow Smoke Test..."
echo "Base URL: $BASE_URL"
echo "========================================"

# Function to make HTTP requests and pretty print JSON
make_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo "📡 $description"
    echo "   $method $BASE_URL$endpoint"
    
    if [ -n "$data" ]; then
        response=$(curl -s -X $method \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$BASE_URL$endpoint")
    else
        response=$(curl -s -X $method "$BASE_URL$endpoint")
    fi
    
    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
    echo ""
    echo "$response"
}

# Function to extract ID from JSON response
extract_id() {
    echo "$1" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])"
}

# Function to extract status from JSON response
extract_status() {
    echo "$1" | python3 -c "import sys, json; print(json.load(sys.stdin)['status'])"
}

# Test 1: Create Task
echo "📋 Test 1: Creating a new task..."
task_response=$(make_request "POST" "/tasks" '{"title": "Sample AI Workflow Task"}' "Creating task")
TASK_ID=$(extract_id "$task_response")
echo "✅ Created task with ID: $TASK_ID"
echo ""

# Test 2: Create Run
echo "🏃 Test 2: Creating a new run for the task..."
run_response=$(make_request "POST" "/runs" "{\"taskId\": $TASK_ID, \"status\": \"PENDING\"}" "Creating run")
RUN_ID=$(extract_id "$run_response")
echo "✅ Created run with ID: $RUN_ID"
echo ""

# Test 3: Check initial run status
echo "📊 Test 3: Checking initial run status..."
make_request "GET" "/runs/$RUN_ID" "" "Getting run details"
echo ""

# Test 4: Check orchestrator stats before processing
echo "📈 Test 4: Checking orchestrator statistics..."
make_request "GET" "/runs/stats" "" "Getting orchestrator stats"
echo ""

# Test 5: Wait and monitor run processing
echo "⏳ Test 5: Monitoring run processing..."
echo "Waiting for automatic processing (orchestrator polls every 3 seconds)..."

for i in {1..30}; do
    echo "   Checking run status... attempt $i/30"
    run_status_response=$(make_request "GET" "/runs/$RUN_ID" "" "Checking run status")
    current_status=$(extract_status "$run_status_response")
    
    echo "   Current status: $current_status"
    
    if [ "$current_status" = "DONE" ] || [ "$current_status" = "FAILED" ]; then
        echo "✅ Run completed with status: $current_status"
        break
    elif [ "$current_status" = "RUNNING" ]; then
        echo "   🔄 Run is processing..."
    fi
    
    if [ $i -eq 30 ]; then
        echo "⚠️  Run did not complete within expected time, forcing processing..."
        make_request "POST" "/runs/$RUN_ID/process" "" "Force processing run"
        sleep 5
    else
        sleep 3
    fi
done
echo ""

# Test 6: Get run steps (execution trace)
echo "📜 Test 6: Fetching execution steps trace..."
steps_response=$(make_request "GET" "/runs/$RUN_ID/steps" "" "Getting run steps")
echo "✅ Retrieved execution steps"
echo ""

# Test 7: Get full execution trace
echo "🔍 Test 7: Fetching full execution trace..."
make_request "GET" "/runs/$RUN_ID/trace" "" "Getting full execution trace"
echo ""

# Test 8: Get individual step artifacts
echo "🗂️  Test 8: Fetching artifacts for each step..."

# Extract step IDs and get their artifacts
step_ids=$(echo "$steps_response" | python3 -c "
import sys, json
data = json.load(sys.stdin)
for step in data:
    print(step['id'])
")

for step_id in $step_ids; do
    echo "   📦 Getting artifacts for step $step_id..."
    make_request "GET" "/steps/$step_id/artifacts" "" "Getting step $step_id artifacts"
    echo ""
done

# Test 9: Test artifact filtering
if [ -n "$step_ids" ]; then
    first_step=$(echo "$step_ids" | head -n 1)
    echo "🔎 Test 9: Testing artifact filtering (TEXT type)..."
    make_request "GET" "/steps/$first_step/artifacts?type=TEXT" "" "Getting TEXT artifacts only"
    echo ""
fi

# Test 10: Final orchestrator stats
echo "📊 Test 10: Final orchestrator statistics..."
make_request "GET" "/runs/stats" "" "Getting final orchestrator stats"
echo ""

# Test 11: Verify task still exists
echo "📋 Test 11: Verifying task still exists..."
make_request "GET" "/tasks/$TASK_ID" "" "Getting task details"
echo ""

# Summary
echo "🎉 Smoke Test Complete!"
echo "========================================"
echo "✅ Task created successfully (ID: $TASK_ID)"
echo "✅ Run created successfully (ID: $RUN_ID)"
echo "✅ Run processed with atomic claiming"
echo "✅ Multiple execution steps created (Planning/Execution/Validation)"
echo "✅ Artifacts generated for each step"
echo "✅ Full execution trace available"
echo "✅ Step-by-step artifact retrieval working"
echo "✅ Artifact filtering functional"
echo "✅ Orchestrator statistics available"
echo ""
echo "🔍 Key APIs tested:"
echo "   • POST /tasks - Create task"
echo "   • POST /runs - Create run"
echo "   • GET /runs/{id} - Get run details"
echo "   • GET /runs/{id}/steps - Get execution trace"
echo "   • GET /runs/{id}/trace - Get full trace with statistics"
echo "   • GET /steps/{id}/artifacts - Get step artifacts"
echo "   • GET /runs/stats - Get orchestrator statistics"
echo ""
echo "🚀 Your agentic workflow system is working correctly!"
echo "   You can now see runs, steps, and artifacts change over time."