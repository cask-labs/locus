# Testing Rules

*   **Unit Tests:** Write unit tests for all business logic and view models to ensure individual components work as expected.
*   **Instrumentation Tests:** Implement instrumentation tests for Android UI components and integration points.
*   **Infrastructure Validation:** Validate CloudFormation templates using linting tools before deployment.
*   **Test Coverage:** Aim for high test coverage on critical paths and complex logic.
*   **Mutation Testing:**
    *   **Scope:** Run mutation tests on the Domain Layer (`:core:domain`) to verify test quality.
    *   **Execution:** Run `./gradlew :core:domain:pitest` to generate reports in `core/domain/build/reports/pitest/`.
    *   **History:** The project uses incremental analysis to speed up execution.
        *   The history file is `core/domain/pitest-history.bin`.
        *   After significant changes or a successful run, execute `./gradlew :core:domain:updatePitestBaseline` and commit the updated binary file to git.
*   **Continuous Integration:** Automate the execution of tests on every pull request to catch regressions early.
