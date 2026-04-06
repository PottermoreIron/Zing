---
description: "Use when building or changing REST controllers, public APIs, or OpenAPI contracts in Zing. Covers transport-only controllers, unified Result<T>, request validation, standard errors, API security, and contract documentation. Trigger words: REST, controller, OpenAPI, facade, operationId, tags, Result<T>, 控制器, 接口, 契约, 文档."
name: "Zing API And OpenAPI Rules"
applyTo: "**/interfaces/rest/**/*.java, **/*Controller.java"
---

# API And OpenAPI Rules

## API Layer

- Controllers handle transport concerns only: request parsing, validation, mapping, delegation, and HTTP status codes.
- Enforce authorization in the service or application layer, not only at controllers.
- Public APIs require rate limiting, explicit CORS allowlists, and HTTPS.
- Never hardcode or log secrets, tokens, or PII.
- Encrypt sensitive data at rest when required and mask sensitive data in logs and responses.

## Contract Rules

- OpenAPI is the contract. Keep it generated or code-aligned.
- Each public operation needs summary, operationId, tags, auth, success response, and standard errors.
- Reuse component schemas, document constraints, separate request and response models when needed, hide internal fields, standardize errors, keep examples minimal, and mark deprecations and version breaks.
