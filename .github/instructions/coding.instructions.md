---
description: "Use when modifying code or configuration across the Zing repo and you need repo-wide coding priorities, simplicity rules, naming rules, and commit hygiene. Trigger words: coding standard, convention, naming, refactor, config, 规范, 约定, 命名, 重构, 配置."
name: "Zing Core Rules"
applyTo: "**/*.java, **/*.xml, **/*.yml, **/*.yaml, **/*.properties, **/*.sql, **/pom.xml"
---

# Core Rules

## Priorities

- Order: correctness > security > simplicity > maintainability > performance.
- Follow repo conventions first. Change patterns only with clear benefit.
- Make the smallest coherent change. Fix root causes, not symptoms.

## General

- KISS, DRY, SOLID. Prefer the simplest correct design. Extract duplication only when stable.
- Use descriptive full-word names. Avoid cryptic abbreviations except common standards (`id`, `url`, `http`).
- Keep methods cohesive and shallow: guard clauses early, low nesting, DTO or record when params grow.
- Comments explain why or tradeoffs only. No narration, TODOs, or dead code.
- Never swallow errors. Catch specific exceptions, add context, rethrow or map centrally.
- Replace magic values with constants, enums, or validated config.
- Default to immutability and restrictive visibility.

## Naming

- Use business terms and full-word names that match the existing module vocabulary.
- Value objects use business names directly, for example `Email` and `MemberId`.

## Git

- Commit only when asked. One logical change per commit.
- Format: `<type>(<scope>): <subject>`.
- Never commit secrets, generated artifacts, or IDE noise.
