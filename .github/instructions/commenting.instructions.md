---
description: "Use when adding, editing, or reviewing comments, Javadoc, or config notes in Zing. Covers English-only comments, when to comment, public API Javadoc, inline comment style, and stale comment removal. Trigger words: comment, Javadoc, inline comment, doc comment, comment, doc comment, annotation comment."
name: "Zing Commenting Rules"
applyTo: "**/*.java, **/*.xml, **/*.yml, **/*.yaml, **/*.properties, **/*.sql"
---

# Commenting Rules

## Default

- Write comments in English only.
- Prefer self-explanatory code over obvious comments.
- Comments explain intent, constraints, invariants, compatibility, side effects, or tradeoffs; never narrate code steps.
- Keep comments short and current. Update or remove them in the same change.
- Keep repository terms consistent. Use `nickname`, not `username`, except for verified legacy surfaces.

## Javadoc

- Add Javadoc only to public contracts or boundaries that are easy to misuse.
- Start with one summary sentence. Add one short paragraph only when needed.
- Use `@param`, `@return`, and `@throws` only for non-obvious semantics.
- Put payload examples in OpenAPI or external docs, not large Javadoc blocks.
- Preserve the file's existing metadata style.

## Inline Comments

- Use inline comments only for decisions code cannot express.
- Put comments above the block unless the note applies to one literal or field.
- One comment should explain one decision.
- Keep wording factual. Avoid conversational notes, history, and TODO prose.

## By File Type

- Java: prefer Javadoc for public contracts and short `//` comments for local rationale.
- SQL, XML, YAML, and properties: explain operational impact, migration constraints, default rationale, or environment-specific behavior.
- Tests: prefer expressive names and assertions; comment only for non-obvious fixtures, protocol quirks, or timing constraints.

## Never

- Do not restate the code.
- Do not keep commented-out code or migration breadcrumbs.
- Do not use comments to excuse weak naming or deep nesting.
- Do not leave stale comments.
