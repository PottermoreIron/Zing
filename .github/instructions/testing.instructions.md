---
description: "Use when writing or refactoring tests in Zing. Covers unit tests, @WebMvcTest, @DataJpaTest, mocking external systems, and behavior-focused assertions. Trigger words: unit test, integration test, Mockito, AssertJ, @WebMvcTest, @DataJpaTest, 单元测试, 集成测试, Mock, 断言."
name: "Zing Testing Rules"
applyTo: "**/src/test/**/*.java"
---

# Testing Rules

## Test Scope

- Prefer focused tests such as unit tests, `@WebMvcTest`, and `@DataJpaTest` over full-context tests.
- Mock external systems and unstable boundaries.
- Test behavior and outcomes, not implementation details.

## Coverage Focus

- Cover business rules, validation branches, transaction boundaries, mapping behavior, and error handling where behavior can regress.
