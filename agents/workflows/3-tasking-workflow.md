# Workflow: Tasking Breakdown

## Purpose

Decompose the work outlined in the provided architecture discovery into the smallest practical set of sequential tasks.

**Output:**
A single document: `./agents/ephemeral/{feature}-tasking/tasking.md`

## Workflow Steps

### Step 0 — Analyze and Recommend Breakdown

**Analyze first, recommend second.**

1.  **Analyze** the architecture discovery directory.
2.  **Formulate** a recommended task breakdown.
    - Arrange tasks sequentially based on dependencies.
    - Make each task independently committable, testable, and verifiable.
    - Keep the total number of tasks minimal.
    - Consolidate related changes that logically belong in a single commit.
3.  **Present** the breakdown to the user for confirmation.

### Step 1 — Create Tasking Document

Once the breakdown is confirmed, write the `tasking.md` file.

For each task, document:
1.  Task title and purpose
2.  Which behaviors or requirements it fulfills
3.  Steps to validate successful completion

**Constraint:**
- The end goal is the **document**, not the execution of the tasks.
- Do not include granular code-level implementation details.
