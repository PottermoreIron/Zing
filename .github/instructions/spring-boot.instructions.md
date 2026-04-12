---
description: "Use when working on Spring Boot controllers, services, application services, configuration, validation, transactions, async execution, or REST response models in Zing. Trigger words: @Service, @RestController, @Valid, @Transactional, @ConfigurationProperties, @Async, Result<T>, Spring Boot, config, validation, transaction, controller."
name: "Zing Spring Boot Rules"
applyTo: "**/application/**/*.java, **/interfaces/**/*.java, **/*Controller.java, **/*Service.java, **/*AppService.java, **/*Config.java, **/*Configuration.java, **/*Properties.java, **/*.yml, **/*.yaml, **/*.properties"
---

# Spring Boot Rules

## Components

- Constructor injection only. No field injection.
- Controllers handle transport only: validation, mapping, delegation, and status codes. Do not place business logic in controllers.
- Services handle business rules and orchestration. Repositories handle persistence only.
- Validate request DTOs with `@Valid`. Keep business validation in the service or application layer.
- Use a global `@RestControllerAdvice` for exception mapping.

## Configuration And Transactions

- Prefer `@ConfigurationProperties` with `@Validated` over scattered `@Value` injection.
- Put `@Transactional` on service or application methods.
- Use `readOnly = true` for query flows.
- Never perform remote calls inside transactions.
- Use `@Async` only with a named executor and across proxy boundaries.

## API Response Model

- REST responses use unified `Result<T>` with `code`, `message`, and `data`.
