# Approach: Joint Architecture Exploration

## Intent

Work through architectural concepts using concise documentation, ensuring alignment on strategy and shared buy-in before code development begins.

**Core Values:**

- **Analysis-driven** - Analyze requirements deeply to form solid recommendations
- **Proactive, not reactive** - Recommend the best path forward rather than asking open-ended questions
- **Minimal yet meaningful docs** - Sufficient detail to explore and decide, nothing more
- **Inquiry-driven** - Expose open questions, competing interests, and design choices; treat implementation specifics as premature

## Scope and Context

Suitable for teams of any size and composition—whether creating backend infrastructure, native mobile applications, or integrated platform features. Adjust language and references to align with your tech choices and design principles.

## Success Criteria (What Success Looks Like)

- 2-3 distinct architectural approaches with explicit tradeoffs explained
- A chosen architecture with clear justification for the choice
- Materials & Inventory (tangible outputs needed without step-by-step instructions)
- Repository footprint (modules, files, and changes required)
- Requirements tracking (link spec items to implementation pieces)
- Team alignment and readiness to move forward
- **Important:** The end goal is a set of documentation artifacts, not implemented code.

## Starting Materials

- Specification sketch or feature request (rough is okay)
- Architectural foundations (design patterns, technology choices, organizational norms already in place)

## Artifact Organization and Retention

Store all discovery outputs under `./agents/ephemeral/{feature}-discovery/` (create folder as needed).
Transition to `docs/` only after team sign-off.

**Required Files (one per purpose):**

```
./agents/ephemeral/{feature}-discovery/
  01-architecture-options.md      # Step 1: Viable paths with core tradeoffs
  02-selection.md                 # Step 2: Side-by-side comparison + decision + why
  03-bill-of-materials.md         # Step 3: What components are needed (boundaries, patterns, setup)
  04-repository-delta.md          # Step 4: Concrete file changes (locations, types, sizes)
  05-spec-alignment.md            # Step 4: How spec requirements map to design pieces
  (optional) 06-adr.md            # Step 5: Architectural Decision Record (when stakes are high)
```

---

## Workflow Steps

### Step 0 — Initial Analysis and Recommendation

**Analyze first, recommend second.**

Recap the specification and your analysis of the problem space:

- "I've reviewed the spec and the codebase. Here is my analysis of the problem..."
- "Based on the constraints (e.g., latency, privacy, dependencies), I recommend focusing on..."

**Then** articulate the problem space:

- Reframe goal and boundaries in 5-10 lines
- Call out constraints
- Name things we don't yet know

**Work product:** Goal + Scope + Open Items + Initial Recommendation

**Checkpoint:** [ ] User has confirmed the problem space and initial recommendation

---

### Step 1 — Outline 2-3 Architectural Paths

**Objective:** Surface distinct solutions with their respective strengths and weaknesses.

For each path:

- Overview (2-3 sentences covering the approach)
- Flow and connections (how data comes in, what happens, what goes out)
- Core decisions (uniqueness handling, error recovery, separation of concerns)
- Advantages and disadvantages
- Possible obstacles and unknowns
- **Scale of effort** (e.g., "6 new files, 2 existing files touched, ~350 lines of code")

**Strategy:**

1. **Path A (Foundation):** Conventional approach following proven patterns in your codebase
2. **Path B (Enhanced):** Overcomes drawbacks in Path A through additional structure
3. **Path C (Different):** Takes a materially different stance (optional; skip if A/B cover the space)

**Work product:** `01-architecture-options.md` displaying all paths in parallel

**Checkpoint:** [ ] 2-3 paths presented clearly; [ ] Benefits and costs obvious; [ ] Effort estimates included

---

### Step 2 — Recommendation and Selection

**Recommend before committing.**

Show a side-by-side view considering:

- Alignment with existing codebase patterns
- How much machinery is required
- Level of exposure to failure modes
- Ease of running and supporting
- Room to grow and modify
- Realistic timeline to completion

**Make a recommendation:**

- "Based on the analysis, I recommend Path {X} because..."
- "Path {Y} is a viable alternative if {Condition}, but Path {X} is superior for {Reason}."

**Then** ask for confirmation to proceed with the recommended path.

**Work product:** `02-selection.md` with side-by-side table + chosen path + explanation

**Checkpoint:** [ ] User approves the recommended path or directs a different choice

---

### Step 3 — Materials and Inventory

**Objective:** Detail what tangible and intangible things the solution requires.

**In this section:**

- **Connection points:**
  - Inbound: form of input, what prompts action, shape of data
  - Outbound: where results go, mechanism of transfer, interface contracts
- **Feature modules:** Packages and namespaces that will be created or extended
- **Horizontal concerns:**
  - Idempotency: how duplicate work is prevented (e.g., "lookup by unique key before proceeding")
  - Durability: how failures are managed (e.g., "retry transient issues, stop on bad input")
  - Data safety/Privacy: how sensitive data is treated (e.g., "never include user data in traces, mandate authorized callers")
  - Monitoring: key data to track (names and varieties only, e.g., "items_processed counter")
  - *For mobile:*
    - Offline mode: how data stays in sync, clash handling
    - Resource use: impact on battery life, data transfer, app footprint
    - Platform standards: OS design conventions, accessibility
    - App states: reactions to suspension, backgrounding, startup
- **Setup/Toggles:** Parameters that can be tuned (names only, no hardcoded values)
- **External libraries/Services:** Third-party packages needed (e.g., "database driver", "REST client", "analytics SDK")
- **Unanswered questions and gaps**

**Out of scope:**

- Low-level retry timing (framework-specific tuning)
- Alert thresholds and escalation rules
- Test coverage strategy
- Deployment and maintenance guides
- Specification linkage (covered separately in Step 4)

**Work product:** `03-bill-of-materials.md`

**Checkpoint:** [ ] Connection points mapped; [ ] Cross-cutting concerns named; [ ] Toolchain/libraries listed; [ ] Unknowns surfaced

---

### Step 4 — Repository Footprint and Alignment

**Objective:** Pin down exact file locations and how spec needs connect to implementation.

**Part A: Repository Footprint** (`04-repository-delta.md`)

**Show:**

- **File roster:**
  - Location (module path, filename)
  - Category (based on your patterns—e.g., interface, logic layer, connector, handler, business rules, screen model, data access, config)
  - Intent (one line per file)
  - Recycled or brand new
  - Size estimate (lines)
- **Directory sketch:** Tree view of modules and files (max ~20 lines)
- **Sample configuration:** Config block + class/property names
- **Total scope** (all code, all tests)

**Exclude:**

- Test files from the roster (account for them in totals though)
- Infrastructure config details
- Generated or boilerplate

**Part B: Spec Mapping** (`05-spec-alignment.md`)

**Show:**

- **Requirement roster:**
  - Spec requirement label (e.g., F1-F3, F4-F6)
  - What the requirement says
  - Which module(s) handle it (path or name)
  - Remarks (e.g., "Extends existing gateway", "Custom implementation")
- **Completeness report:** How many spec requirements are handled

**Work products:** `04-repository-delta.md` + `05-spec-alignment.md`

**Checkpoint:** [ ] File paths accurate; [ ] New vs. existing clear; [ ] Tree organized logically; [ ] All spec items accounted for

---

### Step 5 — Architecture Narrative (Optional)

**Objective:** Record the thinking for future reflection.

Lightweight doc that covers:

- The situation and the tension
- Possibilities examined
- What was picked and why
- Upsides, downsides, and neutral impacts
- Risks we accept

**Work product:** Decision narrative document

**Checkpoint:** [ ] Narrative captures the essential decision points

---

## Output Artifacts Overview

**Total: 5-6 documents** (in workflow sequence)

1. `01-architecture-options.md` (Step 1)
   - Boundaries and goals
   - Path A, B, (C) with scope estimates

2. `02-selection.md` (Step 2)
   - Comparison matrix
   - Chosen path and rationale

3. `03-bill-of-materials.md` (Step 3)
   - Connection points, module roots, patterns
   - Parameters, dependencies
   - Open items

4. `04-repository-delta.md` (Step 4)
   - File table with paths and purposes
   - Directory layout
   - Sample configuration
   - Total lines estimate

5. `05-spec-alignment.md` (Step 4)
   - Spec requirement mapping (requirement → component)
   - Coverage percentage

6. `06-adr.md` (Step 5, optional)
   - Narrative decision record

---

## Templates

### Architecture Options Document

```markdown
# Exploring Architectures: {Feature Name}

## Goals and Boundaries

**What we're building:** {1-2 sentences}

**What's included:**
- {item}
- {item}

**What's deferred:**
- {item}

**Rules and dependencies:**
- {rule}

**Things to figure out:**
- {uncertainty}

---

## Path A: {Label}

**Description:** {2-3 sentences}

**Design:**
- Inbound: {input style, trigger}
- Outbound: {destination(s)}
- Uniqueness: {strategy}
- Failure response: {approach}

**Strengths:**
- {strength}

**Weaknesses:**
- {weakness}

**Obstacles:**
- {obstacle}

---

## Path B: {Label}

{Same structure as Path A}

---

## Path C: {Label} (Optional)

{Same structure as Path A}

---

## Comparison Grid

| Dimension | Path A | Path B | Path C |
|-----------|--------|--------|--------|
| How well it fits our patterns | | | |
| Engineering overhead | | | |
| Brittleness and risk | | | |
| Maintenance friendliness | | | |
| Future flexibility | | | |

---

## Selected Path

**Recommendation:** Path {X}

**Why:**
- {reason}
- {reason}

**Risks we're accepting:**
- {risk detail + how we'll manage it}

**Things we're pushing off:**
- {deferred choice}
```

### Bill of Materials

```markdown
# Materials & Inventory: {Feature Name}

## Design Pattern

{Selected path label}

## Connection Points

**Incoming:**
- Form: {REST/gRPC/events/click/system signal/etc.}
- Trigger: {what causes this to activate}
- Format: {data shape, reference, or schema}

**Outgoing:**
- Destination: {system/database/screen/etc.}
- Method: {TCP/local call/file/etc.}
- Interface: {what we're calling on the other side}

## Module Breakdown

- `{package/module path}/` - {responsibility}

## Cross-Cutting Patterns

**Handling duplicates:**
- Approach: {e.g., "verify identity key before starting"}

**Handling trouble:**
- Recover: {e.g., "resend on timeout, abandon on format error"}
- Stop: {e.g., "refuse bad requests, stop on permission denied"}

**Protecting information:**
- {e.g., "scrub user identity from logs"}
- {e.g., "only allow authenticated, scoped access"}

**Insights and measurement:**
- Metrics (names and units):
  - `{metric.name}` - {kind: counter/timer/gauge} - {purpose}
- Records (themes):
  - {e.g., "correlation ID threaded throughout"}

**Efficiency concerns (if mobile):**
- Power draw: {e.g., "keep background work minimal"}
- Data usage: {e.g., "compress, merge requests"}
- Storage footprint: {e.g., "async load large dependencies"}

**System fit (if mobile):**
- Works offline: {strategy for keeping data fresh, managing disagreements}
- Respects platform: {conventions and accessibility rules}
- App behavior: {how we react when backgrounded, paused, restarted}

## Tuning Options

**Scope:** `app.{module}.{feature}`

**Toggles:**
- `enabled` - activation switch
- `{key}` - {meaning}

## Alignment with Spec

| Specification Item | BOM Link(s) |
|--------------------|-----------|
| {item} | {connection/feature/setting} |

## Unknown or Open

- {question}
```

### Repository Footprint

```markdown
# Repository Footprint: {Feature Name}

## Files Being Added or Changed

| Path | Type | Purpose | Status |
|------|------|---------|--------|
| `path/to/Input` | entry-point/adapter | Where requests arrive | New |
| `path/to/Flow` | workflow/service | Orchestrates steps | New |
| `path/to/Link` | bridge/adapter | Connects outward | New |
| `path/to/Rules` | domain/model | Central logic | New |
| `config/settings` | setup/config | Feature toggles | Modify |

**Approximate size:** ~{number} total code lines (including tests)

## Module Structure

```
{feature}/
  domain/
    {BusinessRules}
  application/
    {Orchestration}
  adapter/
    in/
      {Listener}
    out/
      {Publisher}
  config/
    {Settings}
```

## Configuration Snippet

```yaml
app:
  {module}:
    {feature}:
      enabled: true
      {key}: {example-value}
```

**Config holder:** `app.{module}.{feature}.{Feature}Configuration`

---

## Design Guardrails

These are suggestions—customize to fit your patterns and situation.

**Server-side systems:**
- Keep business logic decoupled; translate between external and internal schemas at seams (adapters/gateways)
- Reject bad input promptly
- Plan retry behavior explicitly; skip retries for validation errors
- Strip personal information from logs; use standardized logging
- Assign metric names deliberately and uniformly

**Native mobile:**
- Isolate business rules from UI machinery and device APIs
- Wire in test doubles (dependency injection)
- Manage system events explicitly (backgrounding, foreground, suspension)
- Persist and reuse data intelligently for offline mode
- Measure actual impact (battery, memory, data)

---

## Working Style

### At Each Pause Point:

1. **Analyze** the current state and requirements.
2. **Formulate** a recommendation or plan.
3. **Present** the recommendation to the user.
4. **Pause** for confirmation or feedback.
5. **Adapt** based on input if needed, or proceed if confirmed.

### Conversation Example:

**You:** "I've studied the spec and identified three distinct directions forward. I recommend Path A because {Reason}. Here are the details..."

**You:** {present the analysis and recommendation}

**You:** "Do you want to proceed with this recommendation?"

**Them:** {confirms or provides feedback}

**You:** {proceed or adjust based on feedback}
```