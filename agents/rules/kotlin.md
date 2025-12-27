# Kotlin Rules

*   **Immutability:** Prefer immutable variables (`val`) and data structures to ensure thread safety and predictability. Use `var` only when necessary.
*   **Concurrency:** Use Kotlin Coroutines for asynchronous operations.
*   **Null Safety:** Leverage Kotlin's type system to handle nullability safely; avoid the non-null assertion operator.
*   **Naming:** Follow standard Kotlin naming conventions for classes (PascalCase), functions (camelCase), and properties (camelCase).
*   **Compose Naming:** Name Composables using PascalCase (Noun-phrases) and event handler lambdas using the `on[Event]` pattern.
*   **Data Classes:** Use data classes for simple data holders to automatically generate utility functions.
*   **Visibility:** Default to `private` or `internal` visibility. Expose only what is strictly necessary for the public API.
*   **File Organization:** Define one class per file unless the types are small DTOs or sealed subclasses tightly coupled to the parent.
*   **Ordering:** Organize class members in this order: Companion Objects, Properties, Initialization blocks, Public Functions, Private Functions, Inner Classes.
*   **Formatting:** Enable trailing commas to reduce diff noise.
