# Workflow: Collaborative Implementation Planning

## Purpose

Develop actionable implementation plans by transforming task breakdowns into executable specifications through deep analysis and recommendation. This workflow bridges the gap between architectural decisions and hands-on development work.

**Key Principles:**

- **Analysis-driven** - Analyze requirements deeply to form solid recommendations
- **Proactive, not reactive** - Recommend the best path forward rather than asking open-ended questions
- **Requirements-driven** - Ensure all implementation work traces back to documented spec behaviors
- **Plan, don't implement** - The goal is a plan document, not code.

## Outcomes (Definition of Done)

- Documented implementation plan with sequential phases and validation checkpoints
- Identified prerequisites for human-driven actions (if applicable)
- Complete mapping of planned components to spec requirements
- Resolution of all identified gaps, ambiguities, and inconsistencies
- Updated discovery documentation and specifications reflecting new insights
- **Output:** A document, not implemented code.

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

1. **Analyze** task description and supporting documentation.
2. **Review** architectural documentation for decisiveness and completeness.
3. **Validate** sufficiency of architectural commitments.

**Analyze and Recommend:**

- "I've examined the architectural documentation and identified a potential ambiguity in {Choice}. I recommend resolving it by {Recommendation}."

**Gate:** [ ] Architectural foundation established; [ ] Discovery materials provide sufficient direction

---

### Phase 1 — Gap Identification & Clarification

**Goal:** Surface and eliminate all uncertainties, inconsistencies, and incomplete information that would affect implementation.

**Actions:**

1. **Examine each component** identified in the task:
   - Data entities, UI elements, State containers, Network integration, Data persistence, Orchestration layers.

2. **Uncover areas of uncertainty:**
   - Incomplete specifications, vague requirements, contradictions.

3. **Analyze and Recommend Resolution:**

   - "For {component}, the spec is unclear about {detail}. Based on the codebase patterns, I recommend we adopt {Approach}."
   - "There is a conflict between {Source A} and {Source B}. I recommend following {Source A} because {Reason}."

4. **Refresh discovery materials** with new clarifications.

**Gate:** [ ] Gaps thoroughly identified; [ ] Recommendations confirmed; [ ] Discovery materials reflect clarifications

---

### Phase 2 — Functionality Classification

**Goal:** Categorize all planned work as new capabilities, enhancements to existing features, or reorganization efforts.

**Actions:**

1. **Review required changes** (New Capabilities, Feature Enhancements, Organizational Refactoring).

2. **Recommend Classification:**

   - "I have analyzed the work and classified it as follows: {Breakdown}. Please confirm this alignment."

**Gate:** [ ] All work assigned to categories; [ ] User confirms categorization aligns with expectations

---

### Phase 3 — Spec Validation

**Goal:** Verify planned implementation satisfies specification requirements; surface coverage gaps, unnecessary elements, and contradictions.

**Actions:**

1. **Establish component-to-behavior mapping.**

2. **Surface misalignments** (Coverage Gaps, Unnecessary Elements, Incompatibilities).

3. **Recommend Alignment:**

   - "I identified a gap in Requirement B7. I recommend adding a {Component} to address this."
   - "Field X appears unnecessary. I recommend removing it from the scope."

4. **Verify bidirectional alignment.**

5. **Reconcile specification** with implementation insights.

**Gate:** [ ] Specification requirements fully mapped to components; [ ] Gaps/bloat/conflicts eliminated; [ ] Specification reflects implementation innovations

---

### Phase 4 — Human Action Extraction

**Goal:** Isolate refactoring work suited to IDE-driven automation as prerequisite actions.

**Actions:**

1. **Spot IDE-friendly refactoring** (Rename, Move, Import updates).

2. **Document human-driven prerequisites** with specific file paths and actions.

3. **Clearly mark section:** "Prerequisites: Human Action Steps - Perform Before Implementation"

**Gate:** [ ] Human prerequisites well-documented; [ ] Verification steps included

---

### Phase 5 — Implementation Plan Generation

**Goal:** Document the implementation approach using the structure that best communicates the work.

**Actions:**

1. **Organize work** into a sensible sequence for this particular task.

2. **Clearly distinguish** between human-driven actions (IDE refactoring) and implementation work.

3. **Provide validation checkpoints** to confirm successful completion of each step.

4. **Recommend Plan:**
   - "I have drafted the following implementation plan based on our analysis. It prioritizes {Strategy}. Please review and confirm."

**Gate:** [ ] Plan clearly communicates the strategy; [ ] User endorses the approach; [ ] Ready to proceed with implementation

---

## Collaboration Pattern

Throughout each phase:

1. **Analyze** the situation deeply.
2. **Formulate** a clear recommendation.
3. **Present** the recommendation to the user.
4. **Pause** for confirmation.
5. **Adapt** based on feedback if necessary.

**Core Collaboration Practices:**

- **Proactive Recommendations** - Do not ask "What should we do?"; ask "Do you agree with this recommendation?"
- **Target specific issues** - Focus recommendations on concrete details.
- **Make reasoning visible** - Explain *why* a recommendation is made.

---

## File Naming Convention

Implementation plan files follow this naming pattern:

`{task-number}-{task-name}-plan.md`

Examples:
- `02-task-2-plan.md`
- `03-task-3-implementation-plan.md`

Location: `./agents/ephemeral/{feature}-tasking/`
