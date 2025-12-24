# Robolectric Rules

*   **Scope:** Utilize Robolectric strictly for Tier 2 Local Integration tests involving Android Framework components such as Services, Workers, and Room DAOs.
*   **Location:** Place all Robolectric tests within the `src/test` source set to execute on the local JVM, distinct from instrumented tests in `src/androidTest`.
*   **Performance:** Prioritize standard JUnit tests for Pure Kotlin logic (Domain Layer, ViewModels) to maximize execution speed; reserve Robolectric for tests requiring an Android Context.
*   **Configuration:** Explicitly configure tests to use SDK 34 to prevent version-specific API failures.
*   **Gradle Setup:** Enable Android resource inclusion in the application module's unit test options and include the AndroidX JUnit extension in data modules.
*   **Database Testing:** Execute Room DAO tests against an in-memory database configuration allowing main thread queries to ensure synchronous reliability.
*   **Environment Simulation:** Leverage Shadows and the Application Provider to control and simulate the Android system state and lifecycle events.
