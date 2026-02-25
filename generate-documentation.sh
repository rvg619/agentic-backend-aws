#!/bin/bash

# Agentic AI System - Documentation Generation Script
# This script helps generate comprehensive documentation for all classes

set -e

echo "🚀 Starting Agentic AI System Documentation Generation..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Project directories
BACKEND_DIR="./backend"
DOCS_OUTPUT_DIR="./documentation"

# Function to print colored output
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

# Check if we're in the right directory
if [ ! -d "$BACKEND_DIR" ]; then
    print_error "Backend directory not found. Please run this script from the project root."
    exit 1
fi

# Create documentation output directory
mkdir -p "$DOCS_OUTPUT_DIR"

print_status "Generating JavaDoc documentation..."

# Navigate to backend directory
cd "$BACKEND_DIR"

# Clean previous builds
print_status "Cleaning previous builds..."
mvn clean > /dev/null 2>&1

# Generate JavaDoc with custom configuration
print_status "Generating JavaDoc HTML documentation..."
if mvn javadoc:javadoc -q; then
    print_success "JavaDoc generation completed successfully"
else
    print_error "JavaDoc generation failed"
    exit 1
fi

# Generate aggregated documentation
print_status "Generating aggregated documentation..."
if mvn javadoc:aggregate -q; then
    print_success "Aggregated documentation generated"
else
    print_warning "Aggregated documentation generation failed (non-critical)"
fi

# Generate Maven site with all reports
print_status "Generating complete project site..."
if mvn site -q; then
    print_success "Maven site generated successfully"
else
    print_warning "Maven site generation failed (non-critical)"
fi

# Copy documentation to main docs folder
cd ..
if [ -d "$BACKEND_DIR/target/site" ]; then
    print_status "Copying documentation to output directory..."
    cp -r "$BACKEND_DIR/target/site/"* "$DOCS_OUTPUT_DIR/"
    print_success "Documentation copied to $DOCS_OUTPUT_DIR"
fi

# Generate class documentation checklist
print_status "Generating documentation checklist..."
cat > "$DOCS_OUTPUT_DIR/documentation-checklist.md" << 'EOF'
# Documentation Checklist

## Core Classes Documentation Status

### AI Agents Package
- [ ] PlannerAgent.java - **High Priority**
- [ ] ExecutorAgent.java - **High Priority**  
- [ ] CriticAgent.java - ✅ **Completed**

### Orchestration Engine
- [ ] RunOrchestrator.java - **High Priority**

### Controllers (API Layer)
- [ ] TaskController.java - **High Priority**
- [ ] RunController.java - **High Priority**
- [ ] GlobalExceptionHandler.java - Medium Priority

### Business Services
- [ ] TaskService.java - **High Priority**
- [ ] RunService.java - **High Priority**
- [ ] StepService.java - **High Priority**
- [ ] ArtifactService.java - Medium Priority

### Data Layer
- [ ] Task.java (Entity) - Medium Priority
- [ ] Run.java (Entity) - Medium Priority
- [ ] Step.java (Entity) - Medium Priority
- [ ] Artifact.java (Entity) - Medium Priority
- [ ] TaskRepository.java - Medium Priority
- [ ] RunRepository.java - Medium Priority
- [ ] StepRepository.java - Medium Priority
- [ ] ArtifactRepository.java - Medium Priority

### DTOs
- [ ] CreateTaskRequest.java - Medium Priority
- [ ] CreateRunRequest.java - Medium Priority
- [ ] TaskResponse.java - Medium Priority
- [ ] RunResponse.java - Medium Priority
- [ ] StepResponse.java - Medium Priority
- [ ] ArtifactResponse.java - Medium Priority
- [ ] RunMetricsResponse.java - Medium Priority

### Configuration
- [ ] AsyncConfig.java - Low Priority
- [ ] CorsConfig.java - Low Priority

### LLM Integration
- [ ] LlmClient.java - **High Priority**

## Documentation Quality Metrics

### Current Status
- **Completed**: 1/25+ classes
- **High Priority Remaining**: 7 classes
- **Medium Priority Remaining**: 15+ classes
- **Low Priority Remaining**: 2+ classes

### Next Steps
1. Document RunOrchestrator (most critical)
2. Document remaining AI agents (PlannerAgent, ExecutorAgent)
3. Document API controllers (TaskController, RunController)
4. Document core services (TaskService, RunService, StepService)
5. Document LLM integration layer

EOF

print_success "Documentation checklist created"

# Generate documentation metrics
print_status "Analyzing documentation coverage..."

# Count total Java files
TOTAL_JAVA_FILES=$(find "$BACKEND_DIR/src/main/java" -name "*.java" | wc -l)

# Count files with JavaDoc (rough estimate by looking for /** patterns)
DOCUMENTED_FILES=$(find "$BACKEND_DIR/src/main/java" -name "*.java" -exec grep -l "/\*\*" {} \; | wc -l)

# Calculate coverage percentage
COVERAGE_PERCENT=$((DOCUMENTED_FILES * 100 / TOTAL_JAVA_FILES))

# Determine progress status
if [ $COVERAGE_PERCENT -ge 95 ]; then
    PROGRESS_STATUS="✅ Goal Achieved"
else
    PROGRESS_STATUS="🎯 In Progress"
fi

cat > "$DOCS_OUTPUT_DIR/documentation-metrics.md" << EOF
# Documentation Metrics Report

**Generated**: $(date)

## Coverage Statistics
- **Total Java Files**: $TOTAL_JAVA_FILES
- **Documented Files**: $DOCUMENTED_FILES  
- **Coverage Percentage**: $COVERAGE_PERCENT%

## Quality Goals
- **Target Coverage**: 95%
- **Remaining Files**: $((TOTAL_JAVA_FILES - DOCUMENTED_FILES))
- **Progress to Goal**: $PROGRESS_STATUS

## Documentation Locations
- **JavaDoc HTML**: [./apidocs/index.html](./apidocs/index.html)
- **Project Site**: [./index.html](./index.html)
- **Source Code**: [GitHub Repository](https://github.com/yourusername/agentic-backend-aws)

## Quick Access Links
- [AI Agents Documentation](./apidocs/com/rajathgoku/agentic/backend/agent/package-summary.html)
- [Orchestration Engine](./apidocs/com/rajathgoku/agentic/backend/engine/package-summary.html)
- [REST API Controllers](./apidocs/com/rajathgoku/agentic/backend/controller/package-summary.html)
- [Business Services](./apidocs/com/rajathgoku/agentic/backend/service/package-summary.html)

EOF

print_success "Documentation metrics generated"

# Create README for documentation folder
cat > "$DOCS_OUTPUT_DIR/README.md" << 'EOF'
# Agentic AI System Documentation

This directory contains comprehensive documentation for the Agentic AI Multi-Agent Orchestration System.

## 📚 Documentation Contents

### API Documentation
- **[JavaDoc API Reference](./apidocs/index.html)** - Complete API documentation
- **[Project Site](./index.html)** - Maven-generated project site with reports

### Development Guides
- **[Documentation Checklist](./documentation-checklist.md)** - Track documentation progress
- **[Documentation Metrics](./documentation-metrics.md)** - Coverage statistics and quality metrics

### Architecture Overview
- **AI Agents**: Multi-agent system with Planner, Executor, and Critic agents
- **Orchestration Engine**: Manages complex multi-step AI workflows
- **REST API**: Spring Boot controllers for task and run management
- **Data Layer**: JPA entities and repositories for persistence

## 🚀 Quick Start

1. **View API Documentation**: Open `./apidocs/index.html` in your browser
2. **Check Project Reports**: Open `./index.html` for comprehensive project information
3. **Review Progress**: Check `documentation-checklist.md` for what's been documented

## 🔧 Regenerating Documentation

To regenerate this documentation:

```bash
# From project root
./generate-documentation.sh
```

## 📊 Current Status

The system includes comprehensive documentation for core components with detailed JavaDoc comments, usage examples, and architectural explanations.

**Last Generated**: Run `./generate-documentation.sh` to update
EOF

print_success "Documentation README created"

# Summary
echo ""
echo "📋 DOCUMENTATION GENERATION SUMMARY"
echo "=================================="
print_success "JavaDoc HTML generated in: $DOCS_OUTPUT_DIR/apidocs/"
print_success "Project site generated in: $DOCS_OUTPUT_DIR/"
print_success "Documentation checklist: $DOCS_OUTPUT_DIR/documentation-checklist.md"
print_success "Coverage metrics: $DOCS_OUTPUT_DIR/documentation-metrics.md"
echo ""
print_status "To view documentation:"
echo "  👉 Open: $DOCS_OUTPUT_DIR/apidocs/index.html"
echo "  👉 Open: $DOCS_OUTPUT_DIR/index.html"
echo ""
print_status "Current documentation coverage: $COVERAGE_PERCENT% ($DOCUMENTED_FILES/$TOTAL_JAVA_FILES files)"
echo ""

if [ $COVERAGE_PERCENT -lt 50 ]; then
    print_warning "Documentation coverage is below 50%. Consider documenting high-priority classes first."
elif [ $COVERAGE_PERCENT -lt 80 ]; then
    print_status "Good progress on documentation! Target: 95% coverage"
else
    print_success "Excellent documentation coverage! Keep up the good work!"
fi

print_success "Documentation generation completed successfully! 🎉"