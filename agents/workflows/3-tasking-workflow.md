Begin with an architecture discovery directory provided by the user. If one hasn't been supplied, **STOP** and request it before continuing.

Decompose the work outlined in the provided architecture discovery into the smallest practical set of sequential tasks. Save this breakdown to ./agents/ephemeral/{feature}-tasking/tasking.md

Requirements:
- Arrange tasks sequentially based on dependencies (each task should build on preceding work)
- Make each task independently committable, testable, and verifiable
- Keep the total number of tasks minimal while respecting their interdependencies
- Consolidate related changes that logically belong in a single commit

For each task, document:
1. Task title and purpose
2. Which behaviors or requirements it fulfills
3. Steps to validate successful completion

Do not include:
- Granular code-level implementation details
- Unit tests for individual configuration settings
- Content beyond the task breakdown itself
