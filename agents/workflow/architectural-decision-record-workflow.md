# Architectural Decision Record (ADR) Workflow

This workflow guides the AI agent in creating an Architectural Decision Record (ADR) for the Locus project.

## Trigger
This workflow is triggered when:
1.  **Explicit Request:** The user asks to "Create an ADR" or "Document a decision".
2.  **Inferred Need:** The agent identifies an architectural change in the code, such as:
    *   Adding a new external library or dependency.
    *   Modifying the database schema or data persistence strategy.
    *   Changing API contracts or network communication patterns.
    *   Deviating from established patterns (e.g., Domain Layer purity).
    *   Significant security or privacy implications (e.g., new permissions).

## Workflow Steps

### 1. Context Gathering (The Interview)
Before drafting the ADR, you **must** interview the user to gather full context. Do not guess. Ask questions to clarify:
*   **The Problem:** What specific problem are we solving?
*   **The Options:** What alternatives were considered?
*   **The Rationale:** Why was this specific solution chosen?
*   **The Constraints:** What technical or business constraints influenced the decision?

### 2. Mobile-Centric Analysis
Reflect on the decision's impact on the specific constraints of the Locus Android project:
*   **Battery Life:** Does this increase background processing or wake locks?
*   **Offline First:** Does this work without network connectivity?
*   **Permissions:** Does this require new sensitive permissions (Location, Background)?
*   **App Size:** Does this significantly increase the APK size?

### 3. Drafting the ADR
Create a new file in `docs/adr/` using a descriptive filename (e.g., `pure-kotlin-domain.md`). Do **not** use numbers.

**Strict Template:**

```markdown
# [Short Title of the Decision]

**Date:** YYYY-MM-DD

## Context
Describe the context and problem statement. What forces are at play? What is the current state?

## Decision
Describe the decision that was made. Be specific.

## Consequences
Describe the resulting context.
*   **Positive:** What did we gain?
*   **Negative:** What trade-offs did we accept?
*   **Risks:** What risks does this introduce?

## Alternatives Considered
List the other options that were evaluated and why they were rejected.
*   **[Alternative A]:** Reason for rejection.
*   **[Alternative B]:** Reason for rejection.
```

### 4. Review & Finalize
1.  Present the draft to the user for review.
2.  Incorporate feedback.
3.  Once approved, save the file to `docs/adr/`.
