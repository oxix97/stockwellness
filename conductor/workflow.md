# Project Workflow

## Guiding Principles

1. **The Plan is the Source of Truth:** All work must be tracked in `plan.md`
2. **The Tech Stack is Deliberate:** Changes to the tech stack must be documented in `tech-stack.md` *before*
   implementation
3. **Test-Driven Development:** Write unit tests before implementing functionality
4. **High Code Coverage:** Aim for >80% code coverage for all modules
5. **User Experience First:** Every decision should prioritize user experience
6. **Non-Interactive & CI-Aware:** Prefer non-interactive commands. Use `CI=true` for watch-mode tools (tests, linters)
   to ensure single execution.

## Task Workflow

All tasks follow a strict lifecycle:

### Standard Task Workflow

1. **Select Task:** Choose the next available task from `plan.md` in sequential order.
2. **Mark In Progress:** Before beginning work, edit `plan.md` and change the task from `[ ]` to `[~]`.
3. **Write Failing Tests (Red Phase):**
    - Create a new test file for the feature or bug fix.
    - Write one or more unit tests that clearly define the expected behavior.
    - **CRITICAL:** Run the tests and confirm that they fail as expected.
4. **Implement to Pass Tests (Green Phase):**
    - Write the minimum amount of application code necessary to make the failing tests pass.
    - Run the test suite again and confirm that all tests now pass.
5. **Refactor (Optional but Recommended):**
    - Improve clarity, remove duplication, and enhance performance.
    - Rerun tests to ensure they still pass.
6. **Verify Coverage:** Run coverage reports (Target: >80%).
7. **Document Deviations:** If implementation differs from tech stack, update `tech-stack.md`.
8. **Update Plan Status:**
    - Read `plan.md`, find the current task, and update its status from `[~]` to `[x]`.
    - **Do NOT add commit hash yet.**
9. **Request Manual Commit (STOP):**
    - **ACTION REQUIRED:** Stop all actions.
    - **Inform User:** "I have completed the task and updated the plan. Please review the changes and perform the commit
      manually."
    - **Command Suggestion:** Provide the suggested commit message to the user.
      e.g., "Suggested commit message: `feat: Add user login functionality`"
    - **WAIT** for the user to commit before proceeding to the next task.

### Phase Completion Verification and Checkpointing Protocol

**Trigger:** Executed immediately after a task is completed that also concludes a phase in `plan.md`.

1. **Announce Protocol Start:** Inform the user that the phase is complete and verification has begun.
2. **Ensure Test Coverage for Phase Changes:**
    - Verify test files exist for all changed code. Create them if missing.
3. **Execute Automated Tests:**
    - Run tests (`npm test`, `./gradlew test`, etc.) and ensure they pass.
4. **Propose Manual Verification:**
    - Provide steps for the user to manually verify the features (e.g., specific `curl` commands or UI checks).
5. **Update Plan Phase:**
    - Update `plan.md` phase heading to mark it as complete.
6. **Request Manual Checkpoint (STOP):**
    - **ACTION REQUIRED:** Stop all actions.
    - **Inform User:** "Phase verification is complete. Please create a checkpoint commit manually."
    - **Command Suggestion:** "Suggested command: `git commit -am 'conductor(checkpoint): Checkpoint end of Phase X'`"
    - **WAIT** for the user to commit.

### Quality Gates

Before marking any task complete, verify:

- [ ] All tests pass
- [ ] Code coverage meets requirements (>80%)
- [ ] Code follows project's code style guidelines (as defined in `code_styleguides/`)
- [ ] All public functions/methods are documented (e.g., docstrings, JSDoc, GoDoc)
- [ ] Type safety is enforced (e.g., type hints, TypeScript types, Go types)
- [ ] No linting or static analysis errors (using the project's configured tools)
- [ ] Works correctly on mobile (if applicable)
- [ ] Documentation updated if needed
- [ ] No security vulnerabilities introduced

## Development Commands

**AI AGENT INSTRUCTION: This section should be adapted to the project's specific language, framework, and build tools.**

### Setup

```bash
# Example: Commands to set up the development environment (e.g., install dependencies, configure database)
# e.g., for a Node.js project: npm install
# e.g., for a Go project: go mod tidy
```

### Daily Development

```bash
# Example: Commands for common daily tasks (e.g., start dev server, run tests, lint, format)
# e.g., for a Node.js project: npm run dev, npm test, npm run lint
# e.g., for a Go project: go run main.go, go test ./..., go fmt ./...
```

### Before Committing

```bash
# Example: Commands to run all pre-commit checks (e.g., format, lint, type check, run tests)
# e.g., for a Node.js project: npm run check
# e.g., for a Go project: make check (if a Makefile exists)
```

## Testing Requirements

### Unit Testing

- Every module must have corresponding tests.
- Use appropriate test setup/teardown mechanisms (e.g., fixtures, beforeEach/afterEach).
- Mock external dependencies.
- Test both success and failure cases.

### Integration Testing

- Test complete user flows
- Verify database transactions
- Test authentication and authorization
- Check form submissions

### Mobile Testing

- Test on actual iPhone when possible
- Use Safari developer tools
- Test touch interactions
- Verify responsive layouts
- Check performance on 3G/4G

## Code Review Process

### Self-Review Checklist

Before requesting review:

1. **Functionality**
    - Feature works as specified
    - Edge cases handled
    - Error messages are user-friendly

2. **Code Quality**
    - Follows style guide
    - DRY principle applied
    - Clear variable/function names
    - Appropriate comments

3. **Testing**
    - Unit tests comprehensive
    - Integration tests pass
    - Coverage adequate (>80%)

4. **Security**
    - No hardcoded secrets
    - Input validation present
    - SQL injection prevented
    - XSS protection in place

5. **Performance**
    - Database queries optimized
    - Images optimized
    - Caching implemented where needed

6. **Mobile Experience**
    - Touch targets adequate (44x44px)
    - Text readable without zooming
    - Performance acceptable on mobile
    - Interactions feel native

## Commit Guidelines

### Message Format

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Formatting, missing semicolons, etc.
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `test`: Adding missing tests
- `chore`: Maintenance tasks

### Examples

```bash
git commit -m "feat(auth): Add remember me functionality"
git commit -m "fix(posts): Correct excerpt generation for short posts"
git commit -m "test(comments): Add tests for emoji reaction limits"
git commit -m "style(mobile): Improve button touch targets"
```

## Definition of Done

A task is complete when:

1. All code implemented to specification
2. Unit tests written and passing
3. Code coverage meets project requirements
4. Documentation complete (if applicable)
5. Code passes all configured linting and static analysis checks
6. Works beautifully on mobile (if applicable)
7. Implementation notes added to `plan.md`
8. Changes committed with proper message (Manually performed by user)

## Emergency Procedures

### Critical Bug in Production

1. Create hotfix branch from main
2. Write failing test for bug
3. Implement minimal fix
4. Test thoroughly including mobile
5. Deploy immediately
6. Document in plan.md

### Data Loss

1. Stop all write operations
2. Restore from latest backup
3. Verify data integrity
4. Document incident
5. Update backup procedures

### Security Breach

1. Rotate all secrets immediately
2. Review access logs
3. Patch vulnerability
4. Notify affected users (if any)
5. Document and update security procedures

## Deployment Workflow

### Pre-Deployment Checklist

- [ ] All tests passing
- [ ] Coverage >80%
- [ ] No linting errors
- [ ] Mobile testing complete
- [ ] Environment variables configured
- [ ] Database migrations ready
- [ ] Backup created

### Deployment Steps

1. Merge feature branch to main
2. Tag release with version
3. Push to deployment service
4. Run database migrations
5. Verify deployment
6. Test critical paths
7. Monitor for errors

### Post-Deployment

1. Monitor analytics
2. Check error logs
3. Gather user feedback
4. Plan next iteration

## Continuous Improvement

- Review workflow weekly
- Update based on pain points
- Document lessons learned
- Optimize for user happiness
- Keep things simple and maintainable
