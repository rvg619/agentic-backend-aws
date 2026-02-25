# Agentic AI System - Documentation Guide

## Overview
This guide provides templates and best practices for documenting all classes in the Agentic AI Multi-Agent Orchestration System.

## JavaDoc Standards

### Class-Level Documentation Template
```java
/**
 * Brief one-line description of the class purpose.
 * 
 * <p>Detailed description explaining what this class does, how it fits into the system,
 * and any important behavioral notes or usage patterns.</p>
 * 
 * <h2>Key Responsibilities:</h2>
 * <ul>
 *   <li>Primary responsibility 1</li>
 *   <li>Primary responsibility 2</li>
 *   <li>Primary responsibility 3</li>
 * </ul>
 * 
 * <h2>Integration:</h2>
 * <p>Describe how this class integrates with other system components.</p>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Provide a clear, realistic usage example
 * ClassName instance = new ClassName(dependencies);
 * ReturnType result = instance.primaryMethod(parameters);
 * }</pre>
 * 
 * @author Your Name
 * @version 1.0
 * @since 1.0
 * @see RelatedClass1
 * @see RelatedClass2
 */
```

### Method Documentation Template
```java
/**
 * Brief one-line description of what this method does.
 * 
 * <p>Detailed description of the method's behavior, including any side effects,
 * preconditions, postconditions, and important implementation details.</p>
 * 
 * @param paramName description of the parameter, including constraints
 * @param anotherParam description with null handling if applicable
 * @return description of return value and possible return states
 * @throws SpecificException when this exception is thrown and why
 * @throws AnotherException description of this exception scenario
 * 
 * @see #relatedMethod(String)
 * @see OtherClass#relevantMethod()
 */
```

## Documentation Generation Commands

### Generate JavaDoc
```bash
# Generate JavaDoc for the entire project
cd backend
mvn javadoc:javadoc

# Generate aggregated JavaDoc with custom styling
mvn javadoc:aggregate

# Generate JavaDoc JAR for distribution
mvn javadoc:jar
```

### Generate Complete Site Documentation
```bash
# Generate complete project site with all reports
mvn site

# Generate site with dependency reports
mvn site:site

# View generated documentation
open target/site/index.html
```

## Class Documentation Priorities

### High Priority (Core System)
1. **RunOrchestrator** - Main orchestration engine
2. **PlannerAgent, ExecutorAgent, CriticAgent** - Core AI agents
3. **TaskController, RunController** - API endpoints
4. **TaskService, RunService, StepService** - Business logic

### Medium Priority (Supporting Infrastructure)
1. **Entity classes** - Data models
2. **Repository interfaces** - Data access
3. **DTO classes** - API contracts
4. **Configuration classes** - System setup

### Low Priority (Utilities)
1. **Exception handlers** - Error management
2. **Utility classes** - Helper functions
3. **Test classes** - Testing infrastructure

## Documentation Standards by Package

### Agent Package (`/agent/`)
- Focus on AI agent behavior and LLM integration
- Document prompt engineering approaches
- Explain agent coordination patterns
- Include performance characteristics

### Engine Package (`/engine/`)
- Document orchestration algorithms
- Explain concurrency and threading models
- Detail error recovery mechanisms
- Include scalability considerations

### Controller Package (`/controller/`)
- Document API endpoints and HTTP contracts
- Include request/response examples
- Detail error handling and status codes
- Provide OpenAPI/Swagger integration

### Service Package (`/service/`)
- Focus on business logic and transaction boundaries
- Document service layer patterns
- Explain validation and error handling
- Include performance and caching notes

### Entity Package (`/entity/`)
- Document JPA relationships and constraints
- Explain database schema decisions
- Detail lifecycle management
- Include query performance notes

## Automated Documentation Tools

### IntelliJ IDEA Templates
Create custom JavaDoc templates in IntelliJ:
1. File → Settings → Editor → Live Templates
2. Add template for class documentation
3. Add template for method documentation

### VS Code Extensions
Recommended extensions:
- Java Documentation Generator
- JavaDoc Tools
- Auto JavaDoc

### IDE Shortcuts
- **IntelliJ**: `/**` + Enter (auto-generates JavaDoc template)
- **VS Code**: `/**` + Enter (with Java extensions)
- **Eclipse**: `/**` + Enter (built-in support)

## Documentation Review Checklist

### For Each Class
- [ ] Class purpose clearly explained
- [ ] Key responsibilities listed
- [ ] Integration points documented
- [ ] Usage example provided
- [ ] All public methods documented
- [ ] Exception handling documented
- [ ] Thread safety noted (if applicable)
- [ ] Performance characteristics mentioned

### For Each Method
- [ ] Purpose clearly stated
- [ ] All parameters documented
- [ ] Return value explained
- [ ] Exceptions documented
- [ ] Side effects noted
- [ ] Null handling specified
- [ ] Thread safety mentioned (if relevant)

## Best Practices

### Writing Style
- Use present tense ("Returns the..." not "Will return the...")
- Be concise but complete
- Use active voice when possible
- Include practical examples
- Link to related classes and methods

### Code Examples
- Make examples realistic and runnable
- Show common usage patterns
- Include error handling where appropriate
- Use meaningful variable names
- Keep examples focused and brief

### Cross-References
- Link to related classes with `@see`
- Reference related methods with `{@link}`
- Include package-level documentation
- Link to external documentation when relevant

## Quality Metrics

### Documentation Coverage Goals
- **Classes**: 100% of public classes documented
- **Methods**: 95% of public methods documented
- **Parameters**: All parameters documented
- **Exceptions**: All checked exceptions documented

### Review Criteria
- Accuracy: Documentation matches implementation
- Completeness: All important aspects covered
- Clarity: Easy to understand for new developers
- Examples: Practical and helpful code samples
- Maintenance: Documentation stays current with code changes