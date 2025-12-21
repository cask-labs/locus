
Ensure the user has given you an architecture discovery directory to work off of.  If not, **STOP** and ask for one.

Break down the work described in the user-provided architecture discovery into the minimal number of sequential tasks.  Please output this to ./agents/ephemeral/{feature}-tasking/tasking.md

Requirements:
- Order tasks by dependency (each task builds on the previous)
- Each task must be independently committable, testable, and validatable
- Minimize task count while respecting dependencies
- Group related changes that can be committed together

For each task, include:
1. Task name and description
2. Which behaviors/requirements it addresses
3. Validation commands to verify the task

Exclude:
- Line-by-line code details
- Unit tests for configuration properties
- Any output other than the task breakdown
