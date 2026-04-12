---
description: "Use when designing or refactoring Zing module boundaries or DDD layers. Covers MVC vs DDD decisions, layer responsibilities, facades, ports, assemblers, repositories, aggregates, and pattern selection. Trigger words: DDD, aggregate, value object, repository, application service, port, assembler, module boundary, aggregate, value object, repository, application service, port, assembler."
name: "Zing Architecture Rules"
applyTo: "**/domain/**/*.java, **/application/**/*.java, **/infrastructure/**/*.java, **/interfaces/**/*.java, **/*Facade*.java, **/*Port*.java, **/*Assembler.java"
---

# Architecture Rules

## Style Selection

- Use MVC for simple CRUD. Use DDD and Hexagonal only when domain complexity justifies it.
- MVC flow: `Controller -> Service -> Repository -> Entity or PO`.
- DDD flow: `Interfaces -> Application -> Domain <- Infrastructure`.

## Layer Boundaries

- Domain has no framework dependencies. Keep only aggregates, entities, value objects, domain services, events, and ports there.
- Application handles orchestration, transactions, commands, queries, DTOs, and assemblers. Do not put core business rules there.
- Infrastructure contains adapters, persistence, external clients, configuration, and port implementations.
- Interfaces contains controllers, consumers, and schedulers only. Delegate immediately.
- No cross-module joins. Cross-module collaboration goes through facades or events.
- Facade modules contain interfaces, DTOs, and constants only.
- Keep one application service per use case or aggregate boundary.

## Naming And Patterns

- Use names such as `XxxController`, `XxxService` or `XxxAppService`, `XxxDomainService`, `XxxRepository`, `XxxRepositoryImpl`.
- Use names such as `XxxPort`, `XxxPortAdapter`, `XxxCommand`, `XxxQuery`, `XxxDTO`, `XxxPO`, `XxxAggregate`, `XxxAssembler`.
- Use patterns only for real volatility. Prefer composition.
- `Strategy` means behavior variants, `Factory` means branching creation, `Adapter` means boundary mismatch, `Decorator` means optional cross-cutting, `State` means state-driven behavior.
- Keep the core concrete, hide volatility behind ports, avoid pattern stacking, and delete low-value indirection.
