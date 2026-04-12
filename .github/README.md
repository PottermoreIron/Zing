# Zing AI Customization Guide

## Purpose

This directory holds Copilot customization files for the Zing repository. The goal is not to dump all knowledge into one file, but to layer persistent context, file-scoped rules, and task-level workflows — reducing conflicts, duplication, and context pollution.

## Structure

| Path                             | Type                  | Role                                                             | When active                               |
| -------------------------------- | --------------------- | ---------------------------------------------------------------- | ----------------------------------------- |
| `copilot-instructions.md`        | Workspace instruction | The single repository-wide persistent instruction                | Always active for every task              |
| `instructions/*.instructions.md` | File instructions     | Scoped rules for specific file types or focal areas              | When matching files are edited or trigger words are hit |
| `skills/*/SKILL.md`              | Skills                | Step-by-step workflows, templates, and reference material        | When a repeatable workflow arises         |
| `agents/*.agent.md`              | Custom agents         | Specialized roles, tool boundaries, and sub-agent delegation     | Only when a dedicated agent is needed     |

## Key Decisions

- This repository uses `copilot-instructions.md` as the single workspace-wide instruction.
- `AGENTS.md` is not introduced as a second workspace instruction to avoid priority conflicts and context duplication.
- If per-directory rule overrides become necessary — for example `auth/` and `im/` adopting distinctly different default development styles — migrate entirely to the `AGENTS.md` model rather than coexisting with `copilot-instructions.md`.
- `.agent.md` is a "custom agent", not an index page or table of contents. Indexes belong in this file; agents are reserved for specialized roles and constrained workflows.

## Project Overview

### What the Project Does

- Zing is a multi-module Java backend platform covering authentication and authorization, the member domain, instant messaging, administration, and an API gateway.
- `framework` provides foundational capability wrappers including common components, distributed ID, Redis, message queues, rate limiting, notification delivery, and code generation.
- `auth` and `member` are the two core business modules where DDD/Hexagonal architecture is applied most rigorously. `im`, `admin`, and `gateway` handle real-time communication, operations back-office, and unified traffic ingress respectively.

### Core Architecture

- Shape: Maven multi-module monorepo.
- Style: Spring Boot microservices + DDD/Hexagonal + MyBatis-Plus + event-driven collaboration.
- Default layering: `interfaces -> application -> domain <- infrastructure`.
- Cross-module collaboration: prefer facades or events; no cross-module persistence coupling.

### Verified Technology Stack

- Java 21
- Spring Boot 3.4.2
- Spring Cloud 2024.0.2
- Spring Cloud Alibaba Nacos Discovery 2023.0.3.3
- MyBatis-Plus 3.5.12
- MySQL + Redis + Flyway
- RabbitMQ/Kafka abstraction starter
- Netty 4.2.3.Final
- JJWT 0.12.6
- SpringDoc OpenAPI 2.8.9
- Leaf 1.0.1-RELEASE
- weixin-java-mp 4.7.7.B

## Module Map

| Module          | Role                                                              |
| --------------- | ----------------------------------------------------------------- |
| `dependencies/` | Unified BOM and version management                               |
| `framework/`    | Shared infrastructure and general-purpose starters               |
| `auth/`         | Authentication, authorization, tokens, permission cache, and auth collaboration |
| `member/`       | Member domain, RBAC, devices, social accounts                    |
| `im/`           | Instant messaging protocol, connection management, messages, and sessions |
| `admin/`        | Administration back-office capabilities                          |
| `gateway/`      | API gateway and unified traffic ingress                          |

## Instructions Directory

| File                                        | Focal Area                                                   | Typical Trigger Words                                               |
| ------------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------------- |
| `instructions/api-openapi.instructions.md`  | REST controllers, OpenAPI contracts, unified response        | `REST`, `controller`, `OpenAPI`, `facade`, `Result<T>`              |
| `instructions/architecture.instructions.md` | DDD layering, module boundaries, assembler, port, facade     | `DDD`, `aggregate`, `repository`, `value object`, `application service` |
| `instructions/coding.instructions.md`       | Repo-wide coding priorities, naming, change scope            | `coding standard`, `convention`, `naming`, `refactor`, `config`    |
| `instructions/commenting.instructions.md`   | Comment policy, Javadoc, English-only comments, stale comment governance | `comment`, `Javadoc`, `doc comment`                    |
| `instructions/java.instructions.md`         | Java language constraints, exceptions, Optional, logging     | `Java`, `Optional`, `record`, `exception`, `logging`               |
| `instructions/persistence.instructions.md`  | MyBatis, SQL, safety, performance                            | `MyBatis`, `mapper XML`, `SQL`, `persistence`                       |
| `instructions/spring-boot.instructions.md`  | Spring Boot components, validation, transactions, config     | `@Service`, `@Transactional`, `config`, `transaction`              |
| `instructions/testing.instructions.md`      | Test layering, mocking, behavior assertions                  | `unit test`, `Mockito`, `AssertJ`, `integration test`               |

## Skills Directory

| Skill                    | Purpose                                                              | Typical Trigger Words                                     |
| ------------------------ | -------------------------------------------------------------------- | --------------------------------------------------------- |
| `skills/api-integration-test/` | API integration tests: endpoint discovery, curl, DB/cache/MQ verification, report | `API test`, `curl testing`, `integration test` |

## When to Add a .agent.md

Consider adding a custom agent under `.github/agents/` only when all of the following apply:

- A stable specialized role is needed, for example "DDD design review", "read-only architecture inspection", or "Mapper XML repair agent".
- Explicit tool boundaries are required, for example allowing only `read/search` or prohibiting terminal/edit.
- The agent is repeatedly delegated to by the main agent with a relatively fixed output format.

Do **not** use `.agent.md` for:

- Project overviews or directory indexes
- General coding conventions
- Simple template storage
- Navigation aids for existing instructions or skills

## Maintenance Rules

- `copilot-instructions.md` contains only facts and invariants that every task depends on; it does not duplicate skill templates or detailed conventions.
- `description` fields consistently use the "Use when..." style and should include representative trigger words to support discoverability.
- `applyTo` should match only files that genuinely need automatic injection; do not widen the scope for convenience.
- Instructions stay single-focus, short and precise; skills stay executable with steps, templates, and reference material.
- When technology versions, module responsibilities, or architecture boundaries change, update both this file and `copilot-instructions.md`.
