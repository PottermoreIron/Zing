# Zing Global Copilot Instructions

## File Layout

- This repository uses only `.github/copilot-instructions.md` as the workspace-wide instruction.
- `.github/README.md` is the index and project overview for the `.github` directory; per-file rules live in `.github/instructions/`, reusable workflows in `.github/skills/`.
- This file contains only stable facts, core boundaries, and repository invariants common to every task.

## Project Overview

- Zing is a Java 21 multi-module backend platform covering authentication and authorization, membership, instant messaging, administration, and an API gateway.
- The repository uses a Maven monorepo structure; core modules: `dependencies`, `framework`, `auth`, `member`, `im`, `admin`, `gateway`.
- Primary technology baseline: Spring Boot 3.4.2, Spring Cloud 2024.0.2, MyBatis-Plus 3.5.12, MySQL, Redis, RabbitMQ/Kafka abstraction, Netty, SpringDoc, Leaf ID, JJWT.

## Default Architecture

- Domain-complex modules follow DDD/Hexagonal first: `interfaces -> application -> domain <- infrastructure`.
- `domain` contains only aggregates, entities, value objects, domain services, domain events, and ports — no Spring or MyBatis dependencies.
- `application` handles use-case orchestration, transaction boundaries, commands, queries, DTOs, and assemblers; it does not own core business rules.
- `infrastructure` owns repository implementations, Mappers, external adapters, configuration, messaging, and ID adapters.
- `interfaces` handles only transport concerns such as REST controllers, internal APIs, consumers, and scheduler entry points.
- Cross-module collaboration must go through facades or events; no cross-module persistence coupling.

## Repository Invariants

- Business IDs are produced by an ID generator; auto-increment is not used.
- Database table names are singular.
- Persistence entities reside in `infrastructure.persistence.entity`.
- Controllers handle only the protocol layer; transactions and orchestration belong in application services.
- Constructor injection is mandatory; field-level `@Autowired` is forbidden.
- All REST responses use the unified `Result<T>` wrapper.

## Usage Rules

- Prefer the smallest coherent change; fix root causes, not surface symptoms.
- Before modifying any specific file, read the matching `.github/instructions/*.instructions.md`.
- When module responsibilities, technology versions, or architecture boundaries change, update both this file and `.github/README.md`.
