# Workflow: Collaborative Architecture Discovery

## Purpose

Quickly iterate on architectural ideas through conversation and lightweight artifacts to reach shared understanding and agreement on approach before implementation.

**Key Principles:**

- **Collaborative, not prescriptive** - Present options, ask questions, incorporate feedback
- **Lightweight artifacts** - Just enough documentation to communicate and decide
- **Iterative** - Easy to revise based on feedback
- **Discovery-focused** - Surface unknowns, risks, and tradeoffs; defer implementation details

## Applicability

This workflow is designed for any software engineering team—whether building backend services, mobile applications, or full-stack features. Adapt the terminology, examples, and patterns to match your technology stack and architectural style.

## Outcomes (Definition of Done)

- 2-3 architectural options with clear tradeoffs
- Selected architecture with rationale
- Bill of Materials (what needs to exist, not how to build it)
- Repository delta (files to add/modify)
- Spec alignment matrix (traceability)
- Shared understanding and agreement to proceed

## Inputs

- Specification or feature description (can be rough)
- Repository architecture constraints (your chosen patterns, tech stack, frameworks)

## Storage Policy

Write all artifacts under `./agents/ephemeral/{feature}-discovery/` (create if absent).
Promote to `docs/` only after approval.

**Required Artifacts (separate files):**

```
./agents/ephemeral/{feature}-discovery/
  01-architecture-options.md      # Step 1: Options with high-level tradeoffs
  02-selection.md                 # Step 2: Comparison table + selected option + rationale
  03-bill-of-materials.md         # Step 3: What needs to exist (edges, themes, config)
  04-repository-delta.md          # Step 4: Exact file changes (paths, kinds, purposes)
  05-spec-alignment.md            # Step 4: Spec behaviors → components mapping
  (optional) 06-adr.md            # Step 5: Architecture Decision Record (if significant)
```

---

## Workflow Steps

### Step 0 — Collaborative Intake

**STOP AND ASK FIRST** before generating anything.

Present the spec back to the user and ask:

- "I've read the spec. Before I propose options, do you have any initial thoughts on approach?"
- "Are there any constraints, preferences, or concerns I should know about?"
- "Should I explore any specific architectural patterns or avoid any?"

**Then** proceed to bound the problem:

- Restate objective and scope in 5-10 lines
- Note constraints (latency/SLA, data sensitivity, dependencies)
- Identify unknowns to resolve

**Deliverable:** Objective + Scope + Unknowns (in conversation or lightweight doc)

**Gate:** [ ] User has provided input or confirmed to proceed

---

### Step 1 — Generate 2-3 Options

**Goal:** Present distinct architectural approaches with clear tradeoffs.

For each option:

- Summary (2-3 sentences)
- Inbound/Outbound approach
- Key design decisions (idempotency, error handling, domain boundaries)
- Pros and cons
- Risks and unknowns
- **High-level file count** (e.g., "8 new files, 3 modified, ~450 LOC")

**Approach:**

1. **Option A (Baseline):** Straightforward solution aligned with existing patterns
2. **Option B (Improved):** Address Option A weaknesses with added complexity
3. **Option C (Alternative):** Orthogonal approach (optional if A/B clearly dominate)

**Deliverable:** `01-architecture-options.md` with all options side-by-side

**Gate:** [ ] 2-3 options presented; [ ] Tradeoffs clear; [ ] File counts estimated

---

### Step 2 — Collaborative Selection

**STOP AND ASK** before selecting.

Present comparison table across:

- Fit to architecture
- Complexity
- Risk
- Operability
- Extensibility
- Time to implement

**Ask user:**

- "Which option resonates with you?"
- "Are there tradeoffs you want to explore further?"
- "Should I refine any option before we decide?"

**Then** document selection with rationale.

**Deliverable:** `02-selection.md` with comparison table + selected architecture + rationale

**Gate:** [ ] User agrees with selection or provides feedback to iterate

---

### Step 3 — Bill of Materials

**Goal:** Enumerate what needs to exist without implementation details.

**Include:**

- **Edges:**
  - Inbound: adapter type, trigger, schema (high-level)
  - Outbound: targets, protocols, contracts (high-level)
- **Feature roots:** Which packages/modules will be added/updated
- **Cross-cutting themes:**
  - Idempotency: strategy (e.g., "check resource by unique identifier before processing")
  - Resilience: themes only (e.g., "retry 5XX, fail-fast on 4XX")
  - Security/PII: themes (e.g., "no PII in logs, required authentication scopes")
  - Observability: key metrics (names + types only, e.g., "events.received counter")
  - *For mobile apps:*
    - Offline capability: sync strategy, conflict resolution
    - Performance: battery usage, network efficiency, app size impact
    - Platform guidelines: Material Design, HIG, accessibility
    - App lifecycle: background task handling, state preservation
- **Configuration:** Property names (prefix + keys, no values yet)
- **Dependencies:** New libraries to add (e.g., "database library", "networking client", "push notification SDK")
- **Open questions**

**Exclude:**

- Detailed retry policies (library-specific configuration)
- Detailed alerting thresholds
- Test plans
- Runbooks
- Spec traceability (moved to separate file in Step 4)

**Deliverable:** `03-bill-of-materials.md`

**Gate:** [ ] Edges listed; [ ] Cross-cutting themes captured; [ ] Dependencies identified; [ ] Open questions captured

---

### Step 4 — Repository Delta & Spec Alignment

**Goal:** Show exactly what files will change and how they map to spec requirements.

**Part A: Repository Delta** (`04-repository-delta.md`)

**Include:**

- **File change table:**
  - Path (exact package/module + filename)
  - Kind (component type based on your architecture—e.g., port, use case, adapter, controller, service, viewmodel, repository)
  - Purpose (1 sentence)
  - Reuse/New
  - Estimated LOC
- **Feature tree:** Module/package structure (≤20 lines)
- **Config example:** Configuration snippet + properties/config class name
- **Total LOC estimate** (production + test code)

**Exclude:**

- Test files in the table (but include in LOC estimate)
- Detailed observability config
- Scaffolded code

**Part B: Spec Alignment Matrix** (`05-spec-alignment.md`)

**Include:**

- **Behavior mapping table:**
  - Spec behavior ID (e.g., B1-B3, B4-B6)
  - Spec requirement summary
  - Component(s) responsible (file path or component name)
  - Notes (e.g., "Reuses existing adapter", "New integration")
- **Coverage summary:** % of spec behaviors covered

**Deliverables:** `04-repository-delta.md` + `05-spec-alignment.md`

**Gate:** [ ] File paths exact; [ ] Reuse vs. new clear; [ ] Feature tree shown; [ ] All spec behaviors mapped

---

### Step 5 — Decision Record (Optional)

**Goal:** Preserve decision with context for future reference.

Lightweight ADR covering:

- Context and problem
- Options considered
- Decision and rationale
- Consequences (positive, negative, neutral)
- Accepted risks

**Deliverable:** ADR document

**Gate:** [ ] ADR captures decision context

---

## File Outputs Summary

**Total: 5-6 files** (numbered for workflow order)

1. `01-architecture-options.md` (Step 1)
   - Objective, scope, unknowns
   - Option A, B, (C) with high-level file counts

2. `02-selection.md` (Step 2)
   - Comparison table
   - Selected architecture + rationale

3. `03-bill-of-materials.md` (Step 3)
   - Edges, feature roots, cross-cutting themes
   - Config keys, dependencies
   - Open questions

4. `04-repository-delta.md` (Step 4)
   - File change table with exact paths
   - Feature tree
   - Config example
   - Total LOC estimate

5. `05-spec-alignment.md` (Step 4)
   - Behavior mapping table (spec → components)
   - Coverage summary

6. `06-adr.md` (Step 5, optional)
   - Lightweight decision record

---

## Templates

### Architecture Options Document

```markdown
# Architecture Discovery: {Feature Name}

## Objective and Scope

**Objective:** {1-2 sentences}

**In Scope:**
- {item}
- {item}

**Out of Scope:**
- {item}

**Constraints:**
- {constraint}

**Unknowns:**
- {unknown}

---

## Option A: {Name}

**Summary:** {2-3 sentences}

**Approach:**
- Inbound: {adapter type, trigger}
- Outbound: {targets}
- Idempotency: {strategy}
- Error handling: {approach}

**Pros:**
- {pro}

**Cons:**
- {con}

**Risks:**
- {risk}

---

## Option B: {Name}

{Same structure as Option A}

---

## Option C: {Name} (Optional)

{Same structure as Option A}

---

## Comparison

| Criteria | Option A | Option B | Option C |
|----------|----------|----------|----------|
| Fit to architecture | | | |
| Complexity | | | |
| Risk | | | |
| Operability | | | |
| Extensibility | | | |

---

## Selected Architecture

**Choice:** Option {X}

**Rationale:**
- {reason}
- {reason}

**Accepted Risks:**
- {risk + mitigation}

**Deferred Decisions:**
- {decision to defer}
```

### Bill of Materials

```markdown
# Bill of Materials: {Feature Name}

## Architecture

{Selected option name}

## Edges

**Inbound:**
- Type: {REST/GraphQL/messaging/user interaction/lifecycle event/etc.}
- Trigger: {what triggers this}
- Schema: {high-level payload structure or reference}

**Outbound:**
- Target: {service/local storage/UI update/etc.}
- Protocol: {HTTP/gRPC/local/etc.}
- Contract: {high-level what we call}

## Feature Roots

- `{package/module path}/` - {purpose}

## Cross-Cutting Themes

**Idempotency:**
- Strategy: {e.g., "check by unique identifier before processing"}

**Resilience:**
- Retry: {e.g., "retry on network errors, transient failures"}
- Fail-fast: {e.g., "fail on validation errors, authorization failures"}

**Security/PII:**
- {e.g., "no PII in logs"}
- {e.g., "required authentication and scopes"}

**Observability:**
- Metrics (names + types):
  - `{metric.name}` - {counter/timer/gauge} - {purpose}
- Logs (themes):
  - {e.g., "correlation ID in all logs"}

**Performance (Mobile-specific, if applicable):**
- Battery impact: {e.g., "minimize background tasks"}
- Network efficiency: {e.g., "batch requests, use compression"}
- App size: {e.g., "lazy-load heavy libraries"}

**Platform Considerations (Mobile-specific, if applicable):**
- Offline capability: {sync strategy, conflict resolution}
- UI guidelines: {platform-specific standards}
- App lifecycle: {background task strategy, state preservation}

## Configuration

**Prefix:** `app.{module}.{feature}`

**Keys:**
- `enabled` - feature flag
- `{key}` - {purpose}

## Spec Traceability

| Spec Requirement | BoM Element(s) |
|------------------|----------------|
| {requirement} | {edge/feature/config} |

## Open Questions

- {question}
```

### Repository Delta

```markdown
# Repository Delta: {Feature Name}

## Files to Add/Modify

| Path | Kind | Purpose | Reuse/New |
|------|------|---------|-----------|
| `path/to/InPort` | port/interface | Entry point interface | New |
| `path/to/UseCase` | use-case/service | Orchestrates flow | New |
| `path/to/Adapter` | adapter/handler | Integration point | New |
| `path/to/Domain` | domain/model | Core business logic | New |
| `config/file` | configuration | Add feature config | Modify |

**Estimated LOC:** ~{number} (including tests)

## Feature Tree

```
{feature}/
  domain/
    {DomainModel}
  application/
    {BusinessLogic}
  adapter/
    in/
      {Listener}
    out/
      {Integration}
  config/
    {Properties}
```

## Configuration Example

```yaml
app:
  {module}:
    {feature}:
      enabled: false
      {key}: {default-value}
```

**Config Class:** `app.{module}.{feature}.{Feature}Config`

---

## Architecture Guardrails

These guardrails are illustrative—adapt based on your architecture pattern and constraints.

**Backend Services:**
- Keep domain independent; map external models at boundaries (ports/adapters)
- Fail-fast on invalid inputs
- Define resilience policies; do not retry validation errors
- Never log PII; use your logging framework
- Define metric names early and consistently

**Mobile Apps:**
- Keep domain logic separate from UI state and platform APIs
- Use dependency injection for testability
- Handle app lifecycle events explicitly (background, foreground, suspended)
- Cache data intelligently for offline support
- Test performance impact (battery, memory, network)

---

## Interaction Pattern

### At Each Stop Point:

1. **Present** what you've discovered/generated
2. **Ask** for user input, preferences, concerns
3. **Wait** for user response
4. **Incorporate** feedback and iterate
5. **Confirm** before moving to next step

### Example Dialogue:

**Agent:** "I've analyzed the spec and see three possible approaches. Before I detail them, do you have any initial thoughts on how this should work?"

**User:** {provides input or says "go ahead"}

**Agent:** {presents options}

**Agent:** "Which option resonates with you? Should I refine any before we decide?"

**User:** {provides feedback}

**Agent:** {iterates or proceeds based on feedback}
