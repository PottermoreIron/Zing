---
description: "Use when writing or refactoring Java in the Zing repo. Covers field design, null handling, Optional, generics, exceptions, records, Lombok, and logging. Trigger words: Java, Optional, record, exception, Lombok, logging, 空值, 异常, 日志."
name: "Zing Java Rules"
applyTo: "**/*.java"
---

# Java Rules

## Java

- Fields are `private final` unless mutation is required. Do not use public fields.
- Return empty collections, not `null`. Use `Optional` only for optional return values.
- Prefer the clearest construct: Stream for transformation, loops for complex or stateful flow.
- Use unchecked custom exceptions for business or programming failures.
- Prefer `record`, `sealed`, enums, generics, immutable factories, and interface types on APIs.
- Do not use raw types.
- Lombok: prefer `@Getter`, `@Builder`, `@RequiredArgsConstructor`. Avoid `@Data` and broad `@Setter`, especially on entities and domain types.

## Logging

- Use SLF4J and Logback only. No `System.out` or stdout stack traces.
- `ERROR`: failed operation needing attention. `WARN`: handled anomaly. `INFO`: major business events. `DEBUG`: diagnostics only.
- Log useful context such as `traceId` and business identifiers. Never log secrets, tokens, or PII.
- Log boundaries, external calls, state changes, and exceptions. Avoid noisy entry or exit logs and tight-loop logging.
