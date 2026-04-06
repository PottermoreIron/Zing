---
description: "Use when modifying MyBatis mapper XML, repository implementations, persistence entities, or SQL in Zing. Covers repository boundaries, query safety, pagination, batching, indexing, N+1 prevention, and database performance. Trigger words: MyBatis, mapper XML, repository implementation, resultMap, SQL, 持久化, Mapper, XML, 查询, 索引."
name: "Zing Persistence Rules"
applyTo: "**/infrastructure/persistence/**/*.java, **/src/main/resources/mapper/**/*.xml, **/mapper/**/*.xml, **/*.sql"
---

# Persistence Rules

## Boundaries

- Repositories handle persistence only.
- Keep PO and mapper concerns inside infrastructure and persistence layers. Do not leak PO types into domain logic.
- Domain and application layers collaborate with repositories through domain abstractions, not mapper details.

## Query Safety

- Use parameterized queries only. Never build SQL or JPQL with string concatenation.
- Validate and sanitize all external input before it reaches persistence boundaries.

## Performance

- Select only needed columns.
- Paginate list queries.
- Batch writes where appropriate.
- Prevent N+1 queries.
- Add indexes for critical predicates and joins.
- Cache only hot, stable, read-heavy data. Set TTLs, include all key discriminators, and invalidate on writes.
- Avoid loading large collections into memory when streaming or pagination will do.
