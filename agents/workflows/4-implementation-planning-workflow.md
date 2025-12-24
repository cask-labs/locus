# Workflow: Collaborative Implementation Planning

## Purpose

Develop actionable implementation plans by transforming task breakdowns into executable specifications through active clarification and alignment with existing requirements. This workflow bridges the gap between architectural decisions and hands-on development work, concentrating on feature-level requirements and specification consistency.

**Key Principles:**

- **Engage actively, don't prescribe** - Consistently solicit input; avoid making unvalidated assumptions
- **Requirements-driven** - Ensure all implementation work traces back to documented spec behaviors; maintain bidirectional updates to specs
- **Feature-focused** - Distinguish between new capabilities, modifications to existing features, and structural reorganization
- **Leverage IDE tooling** - Extract refactoring work best suited to IDE capabilities (moves, renames) as prerequisite actions
- **Continuous refinement** - Keep architectural documentation and specifications synchronized with findings discovered during planning

## Outcomes (Definition of Done)

- Documented implementation plan with sequential phases and validation checkpoints
- Identified prerequisites for human-driven actions (if applicable)
- Complete mapping of planned components to spec requirements
- Resolution of all identified gaps, ambiguities, and inconsistencies
- Updated discovery documentation and specifications reflecting new insights
- Alignment achieved with stakeholders; readiness confirmed to begin development

## Inputs

- Task description derived from task breakdown
- Reference materials (requirements specifications, discovery documentation, organizational wiki, example implementations, visual designs)
- Familiarity with the existing codebase

## Output

Single implementation plan file: `{task-number}-{task-name}-plan.md`

**The plan should contain:**

- Required preconditions (human-driven refactoring steps, if needed)
- Development steps (organized in sensible sequence)
- Alignment mapping (linking planned components to documented behaviors)
- Testing and validation strategy
- Clear definition of completion criteria

**Structure is adaptable** - organize the plan in whatever format best clarifies the work.

---

## Workflow Steps

### Phase 0 — Intake & Architecture Validation

**Goal:** Confirm architectural foundation is established and understood before moving to detailed implementation planning.

**Actions:**

1. Examine task description along with supporting documentation
2. Review architectural documentation for decisiveness and completeness
3. Validate sufficiency of architectural commitments

**STOP AND ASK** whenever architectural clarity is uncertain:

- "I've examined the architectural documentation and need clarification on {specific architectural choice}. Should we resolve this before continuing?"
- "The current architectural documentation leaves {architectural consideration} unaddressed. Would you like to establish this decision now?"

**If architectural foundation is incomplete:**

- Conduct focused architecture assessment (outline options, discuss tradeoffs, confirm direction)
- Document architectural decisions in discovery materials

**Gate:** [ ] Architectural foundation established; [ ] Discovery materials provide sufficient direction

---

### Phase 1 — Gap Identification & Clarification

**Goal:** Surface and eliminate all uncertainties, inconsistencies, and incomplete information that would affect implementation.

**Actions:**

1. **Examine each component** identified in the task:

   - Data entities: Which fields are required? What validation constraints exist?
   - Unique identifiers: What formats apply (UUID, bespoke)? How are they generated?
   - User interface elements: Which screens or views are involved? How does navigation flow?
   - State containers: How is state managed? What data connections exist?
   - Network integration: Which endpoints are involved? What network patterns apply?
   - Data persistence: How is data stored and retrieved?
   - Orchestration layers: What business logic sequencing is needed? What dependencies exist?

2. **Uncover areas of uncertainty:**

   - Incomplete specifications (missing data structure definitions, function signatures, data formats)
   - Vague requirements (unclear expected behavior, multiple valid interpretations)
   - Contradictions across sources (discovery documentation versus specification, specification versus reference code)

3. **STOP AND ASK** to resolve unclear points:

   - "For {component}, I need clarification on: {specific details}"
   - "I've found inconsistent information: {source A} says {statement} while {source B} says {statement}. Which is correct?"
   - "The specification describes {behavior}, but the implementation approach for {detail} isn't specified. What's the intended behavior?"

4. **Keep asking** until understanding is complete

   - Avoid proceeding based on assumptions
   - If clarifications generate new questions, ask immediately

5. **Refresh discovery materials** with new clarifications:
   - Update discovery documentation when clarifications have significance for overall design
   - Omit updates for implementation-level details that don't influence architectural decisions

**Gate:** [ ] Gaps thoroughly identified; [ ] All questions about requirements answered; [ ] Discovery materials reflect clarifications

---

### Phase 2 — Functionality Classification

**Goal:** Categorize all planned work as new capabilities, enhancements to existing features, or reorganization efforts.

**Actions:**

1. **Review required changes:**

   **New Capabilities:**

   - User-facing screens or views not currently implemented
   - State management systems or containers not yet built
   - Network service methods or backend endpoints to be created
   - Data storage structures requiring new schemas
   - Feature behaviors with no parallel in the current implementation

   **Feature Enhancements:**

   - Modifications to user flows or navigation sequences
   - Changes to existing state containers or business logic
   - Updated network service contracts or domain data structures
   - Refined data storage mechanisms or schemas

   **Organizational Refactoring:**

   - Renaming of classes or files
   - Restructuring of modules or packages
   - Changes to import statements and dependencies
   - Code reorganization without altering visible behavior

2. **STOP AND ASK** to confirm classification:

   - "I've categorized the work as:
     - {X} new capabilities: {list}
     - {Y} feature enhancements: {list}
     - {Z} organizational refactoring: {list}
   - Is this breakdown consistent with your understanding?"

3. **Refine classification** based on feedback

**Gate:** [ ] All work assigned to categories; [ ] User confirms categorization aligns with expectations

---

### Phase 3 — Spec Validation

**Goal:** Verify planned implementation satisfies specification requirements; surface coverage gaps, unnecessary elements, and contradictions.

**Actions:**

1. **Establish component-to-behavior mapping:**

   - Document how each component addresses spec requirements (e.g., "B1-B3 → Component X processes incoming events")
   - Summarize the role each component plays in fulfilling requirements

2. **Surface misalignments:**

   **Coverage Gaps:** Spec requirements with no corresponding implementation component

   - "Requirement B7 (offline-first operation) has no planned implementation component"

   **Unnecessary Elements:** Implementation components with no basis in specification

   - "Field X in the UserProfile model isn't required by any specification behavior"

   **Incompatibilities:** Implementation approach conflicts with specification requirements

   - "Implementation assumes real-time API availability, but specification mandates offline-first operation (B1)"

3. **STOP AND ASK** for resolution:

   - "Based on requirements, I suggest:
     - Removing: {unnecessary items}
     - Adding: {missing items}
     - Revising: {conflicting items}
   - Do you concur?"

4. **Verify bidirectional alignment** - STOP AND ASK:

   - "Does the implementation introduce capabilities beyond the specification scope?"
   - "Should we incorporate these additions into the specification?"

5. **Reconcile specification** with implementation insights:
   - Amend specification when implementation introduces features warranting documentation
   - Defer updates for tactical implementation decisions without strategic impact

**Gate:** [ ] Specification requirements fully mapped to components; [ ] Gaps/bloat/conflicts eliminated; [ ] Specification reflects implementation innovations

---

### Phase 4 — Human Action Extraction

**Goal:** Isolate refactoring work suited to IDE-driven automation as prerequisite actions.

**Actions:**

1. **Spot IDE-friendly refactoring:**

   - Renaming of classes or files (IDE "Rename" capability)
   - Relocating packages or modules (IDE "Move" capability)
   - Import path updates (automatically handled by IDE)

2. **Document human-driven prerequisites:**

   - Source and destination file paths with complete specificity
   - Files affected by the change
   - IDE operation to use

3. **Clearly mark section:** "Prerequisites: Human Action Steps - Perform Before Implementation"

**Example:**

````markdown
## Prerequisites: Human Action Steps

Execute these refactoring operations using your IDE before implementation begins:

### Step 1: Rename UserId → UserProfileId

**iOS:**

- **File:** `Sources/Domain/User/UserId.swift`
- **Action:** Xcode "Refactor → Rename"
- **New Name:** `UserProfileId`
- **Affected Files:** 12 files (Xcode updates automatically)
  - UserProfile.swift
  - UserViewModel.swift
  - UserApiClient.swift
  - ...

**Android:**

- **File:** `app/src/main/kotlin/com/example/domain/user/UserId.kt`
- **Action:** Android Studio "Refactor → Rename"
- **New Name:** `UserProfileId`
- **Affected Files:** 12 files (IDE updates automatically)
  - UserProfile.kt
  - UserViewModel.kt
  - UserApiClient.kt
  - ...

**Verification:**

```bash
# iOS
xcodebuild clean build -scheme YourApp

# Android
./gradlew clean build
```
````

**Gate:** [ ] Human prerequisites well-documented; [ ] Verification steps included

---

### Phase 5 — Implementation Plan Generation

**Goal:** Document the implementation approach using the structure that best communicates the work.

**Actions:**

1. **Organize work** into a sensible sequence for this particular task

2. **Clearly distinguish** between human-driven actions (IDE refactoring) and implementation work

3. **Provide validation checkpoints** to confirm successful completion of each step

4. **STOP AND ASK** to validate the proposed approach:
   - "I'm planning to approach this as follows: {summary of strategy and architectural choices}"
   - "Does this direction align with your expectations, or should I revise anything?"
   - Pause for response and guidance

5. **Integrate feedback** into the finalized plan

**The plan structure is flexible** - choose the format that most effectively communicates the work. Avoid imposing a standardized template.

**Gate:** [ ] Plan clearly communicates the strategy; [ ] User endorses the approach; [ ] Ready to proceed with implementation

---

## Collaboration Pattern

Throughout each phase:

1. **Share** analysis results, findings, and recommendations
2. **Pose** targeted questions addressing gaps, decisions, and tradeoffs
3. **Await** stakeholder input (never proceed independently without confirmation)
4. **Integrate** feedback into the plan
5. **Refresh** architectural documentation and specifications as warranted
6. **Validate** shared understanding before moving to subsequent phases

**Core Collaboration Practices:**

- **Maintain regular communication** - Active engagement outweighs minimal communication
- **Base work on explicit information** - Request clarification when uncertain; repeat if clarity remains elusive
- **Target specific issues** - Ask about concrete implementation details rather than abstract concepts
- **Make reasoning visible** - Articulate the logic informing classifications and recommendations
- **Maintain documentation currency** - Ensure architectural records and specifications reflect current understanding

---

## File Naming Convention

Implementation plan files follow this naming pattern:

`{task-number}-{task-name}-plan.md`

Examples:
- `02-task-2-plan.md`
- `03-task-3-implementation-plan.md`

Location: `./agents/ephemeral/{feature}-tasking/`
