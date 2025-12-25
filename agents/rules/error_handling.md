# Error Handling Rules

*   **Fail Fast:** Assert code invariants using standard checks to expose logic errors immediately.
*   **No Silent Failures:** Ensure all exceptions are handled, propagated, or escalated; avoid empty catch blocks.
*   **Structured Propagation:** Use result types to explicitly return failure states from Data and Domain layers.
*   **Graceful Recovery:** Catch operational exceptions at the UI boundary to present user-facing feedback instead of crashing.
