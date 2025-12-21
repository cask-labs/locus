# Workflow: Collaborative Implementation Planning

## Purpose

Transform a task from the task breakdown into a detailed, executable implementation plan through collaborative clarification and spec validation. This workflow sits between architecture discovery and implementation, focusing on functionality-level details and spec alignment.

**Key Principles:**

- **Collaborative, not prescriptive** - Ask questions frequently; never assume
- **Spec-driven** - All work must align to spec behaviors; update spec bidirectionally
- **Functionality-focused** - Distinguish net-new vs. altered functionality
- **Human-friendly refactors** - Extract IDE-optimal work (renames, moves) as prerequisites
- **Iterative** - Update discovery docs and specs with clarifications as we go

## Outcomes (Definition of Done)

- Implementation plan with clear phases and validation steps
- Human action prerequisites identified (if any)
- All components mapped to spec behaviors
- All gaps, ambiguities, and conflicts resolved
- Discovery docs and specs updated with clarifications
- Shared understanding and agreement to proceed with implementation

## Inputs

- Task description from task breakdown
- Supporting documentation (specs, discovery docs, wiki/documentation system, sample code, design files)
- Existing codebase context

## Output

Single implementation plan file: `{task-number}-{task-name}-plan.md`

**The plan should include:**

- Prerequisites (human action steps, if any)
- Implementation steps (in some logical order)
- Spec alignment (map components to behaviors)
- Validation approach
- What "done" looks like

**Format is flexible** - use whatever structure makes the task clear.

---

## Workflow Steps

### Phase 0 — Intake & Architecture Validation

**Goal:** Ensure architectural decisions are clear before diving into implementation details.

**Actions:**

1. Review task description and supporting materials
2. Review discovery docs for architectural clarity
3. Assess if architectural decisions are sufficiently clear

**STOP AND ASK** if you (the agent) feel anything is architecturally unclear:

- "I've reviewed the discovery docs and I'm unclear about {specific architectural decision}. Should we clarify this before proceeding?"
- "The discovery docs don't clearly address {architectural concern}. Should we validate the architecture for this component?"

**If architecture is unclear:**

- Conduct mini-architecture validation (present options, tradeoffs, get selection)
- Update discovery docs with architectural decisions

**Gate:** [ ] Architecture is clear; [ ] Discovery docs are sufficient

---

### Phase 1 — Gap Identification & Clarification

**Goal:** Identify and resolve all ambiguities, conflicts, and missing information needed for implementation.

**Actions:**

1. **Analyze each component** mentioned in the task:

   - Domain models: What fields? What validation rules?
   - Identifiers: What ID formats (UUID, custom)? What factory methods?
   - UI Components: What screens/views? What navigation patterns?
   - View Models: What state management? What data bindings?
   - API Clients: What endpoints? What network layer?
   - Local Storage: What persistence layer?
   - Use cases: What orchestration logic? What dependencies?

2. **Identify gaps:**

   - Missing information (field lists, method signatures, formats)
   - Ambiguous requirements (unclear behavior, multiple interpretations)
   - Conflicting information (discovery vs. spec, spec vs. samples)

3. **STOP AND ASK** clarifying questions:

   - "For {component}, I need to know: {specific questions}"
   - "I see conflicting information about {topic} in {source A} vs {source B}. Which should I follow?"
   - "The spec mentions {behavior} but doesn't specify {detail}. What should the implementation do?"

4. **Continue asking** until all gaps are resolved

   - Don't proceed with assumptions
   - If answers raise new questions, ask those too

5. **Update discovery docs** with clarifications:
   - Update original discovery docs for significant architectural clarifications
   - Skip updates for minor implementation details that don't affect high-level design

**Gate:** [ ] All gaps identified; [ ] All clarifying questions answered; [ ] Discovery docs updated

---

### Phase 2 — Functionality Classification

**Goal:** Classify all work into net-new functionality, altered functionality, and structural refactors.

**Actions:**

1. **Analyze what needs to happen:**

   **Net-New Functionality:**

   - New screens/views that don't exist yet
   - New view models or state management
   - New API client methods or endpoints
   - New local storage schemas
   - New behaviors not present in current codebase

   **Altered Functionality:**

   - Changes to existing screen flows or navigation
   - Updates to existing view models or business logic
   - Modified API contracts or data models
   - Updated persistence or local storage

   **Structural Refactors:**

   - File/class renames
   - Module reorganization or package moves
   - Import updates
   - Code reorganization without behavior change

2. **STOP AND ASK** for validation:

   - "I've identified:
     - {X} net-new components: {list}
     - {Y} alterations to existing functionality: {list}
     - {Z} structural refactors: {list}
   - Does this match your expectations?"

3. **Incorporate feedback** and adjust classification

**Gate:** [ ] All work classified; [ ] User agrees with classification

---

### Phase 3 — Spec Validation

**Goal:** Ensure implementation aligns with spec behaviors; identify gaps, bloat, and conflicts.

**Actions:**

1. **Map components to spec behaviors:**

   - Create behavior-level mapping (e.g., "B1-B3 → Component X handles event parsing")
   - Include short description of what each component addresses

2. **Identify issues:**

   **Gaps:** Spec behaviors not covered by implementation

   - "Behavior B7 (offline caching) is not addressed by any component"

   **Bloat:** Implementation components not required by spec

   - "Field X in the UserProfile model is not used by any spec behavior"

   **Conflicts:** Implementation contradicts spec

   - "Implementation assumes synchronous API calls, but spec requires offline-first behavior (B1)"

3. **STOP AND ASK** for optimization:

   - "Based on the spec, I recommend:
     - Removing: {bloat items}
     - Adding: {gap items}
     - Changing: {conflict items}
   - Do you agree?"

4. **Bidirectional check** - STOP AND ASK:

   - "Are there any new behaviors in this implementation that exceed the spec's boundaries?"
   - "Should these new behaviors be added back to the spec?"

5. **Update spec** with new behaviors:
   - Update original spec for new behaviors that should be documented
   - Skip updates for minor implementation details that don't affect spec-level behavior

**Gate:** [ ] All spec behaviors mapped; [ ] Gaps/bloat/conflicts resolved; [ ] Spec updated with new behaviors

---

### Phase 4 — Human Action Extraction

**Goal:** Extract refactors best handled by IDE tooling into prerequisite steps.

**Actions:**

1. **Identify IDE-optimal refactors:**

   - Class/file renames (IDE "Rename" refactoring)
   - Package moves (IDE "Move" refactoring)
   - Import updates (automatic with IDE refactoring)

2. **Create human action steps:**

   - Exact before/after paths
   - List of affected files
   - Recommended IDE operation

3. **Label clearly:** "Human Action Steps - Complete Before Agent Work"

**Example:**

````markdown
## Prerequisites: Human Action Steps

Complete these refactors using IDE tooling before agent implementation:

### Step 1: Rename UserId → UserProfileId

**iOS:**

- **File:** `Sources/Domain/User/UserId.swift`
- **Action:** Use Xcode "Refactor → Rename"
- **New Name:** `UserProfileId`
- **Affected Files:** 12 files (Xcode will update automatically)
  - UserProfile.swift
  - UserViewModel.swift
  - UserApiClient.swift
  - ...

**Android:**

- **File:** `app/src/main/kotlin/com/example/domain/user/UserId.kt`
- **Action:** Use Android Studio "Refactor → Rename"
- **New Name:** `UserProfileId`
- **Affected Files:** 12 files (IDE will update automatically)
  - UserProfile.kt
  - UserViewModel.kt
  - UserApiClient.kt
  - ...

**Validation:**

```bash
# iOS
xcodebuild clean build -scheme YourApp

# Android
./gradlew clean build
```
````

**Gate:** [ ] Human action steps clearly documented; [ ] Validation commands provided

---

### Phase 5 — Implementation Plan Generation

**Goal:** Document the implementation approach in whatever format makes sense for this task.

**Actions:**

1. **Organize the work** into a logical sequence that makes sense for this task

2. **Separate human actions** (IDE refactors) from agent work clearly

3. **Include validation steps** so you know each step worked

4. **STOP AND ASK** for approach validation:
   - "Here's how I'm thinking about implementing this: {brief summary of approach and key decisions}"
   - "Does this approach make sense, or should I adjust anything?"
   - Wait for approval or feedback

5. **Incorporate feedback** and document the plan

**The plan format is flexible** - use whatever structure makes the task clear. Don't force a rigid template.

**Gate:** [ ] Plan documents the approach clearly; [ ] User approves approach; [ ] Ready for implementation

---

## Collaboration Pattern

At each phase:

1. **Present** findings, analysis, or recommendations
2. **Ask** specific questions about gaps, decisions, tradeoffs
3. **Wait** for user response (never assume or proceed without input)
4. **Incorporate** feedback into the plan
5. **Update** discovery docs or specs as needed
6. **Confirm** understanding before proceeding to next phase

**Key Behaviors:**

- **Ask frequently** - Better to over-communicate than under-communicate
- **Never assume** - If unclear, ask; if still unclear, ask again
- **Be specific** - Ask about concrete details, not vague concepts
- **Show your work** - Explain reasoning behind classifications and recommendations
- **Update upstream** - Keep discovery docs and specs in sync with clarifications

---

## File Naming Convention

Implementation plans should be named:

`{task-number}-{task-name}-plan.md`

Examples:
- `02-task-2-plan.md`
- `03-task-3-implementation-plan.md`

Store in: `./agents/ephemeral/{feature}-tasking/`
