# Style Guide & Naming Conventions

**Related Requirements:** [Design Guidelines](design_guidelines.md), [Kotlin Rules](../../agents/rules/kotlin.md)

This document defines the strict coding style, naming conventions, and resource management rules for the Locus project.

## 1. Resource Naming (XML)

Resources must use **snake_case** with specific prefixes to prevent collisions and improve auto-completion.

| Resource Type | Prefix Convention | Example |
| :--- | :--- | :--- |
| **Layouts** | `activity_`, `fragment_`, `item_`, `view_` | `activity_main.xml`, `item_log_entry.xml` |
| **Strings** | `[feature]_[description]` | `dashboard_status_active`, `settings_title` |
| **Drawables** | `ic_`, `bg_`, `img_` | `ic_gps_fixed.xml`, `bg_gradient.xml` |
| **Colors** | `color_` (Base only) | `color_brand_primary` |
| **Dimens** | `dimen_` | `dimen_spacing_small` |

**Rule:** Do not use generic names like `title` or `error`. Always prefix with the feature context (e.g., `onboarding_error_network`).

## 2. Color System (Compose)

Do not use hardcoded hex values or `Color.Red` in UI code. Always use the **MaterialTheme** semantic tokens.

*   **Correct:** `MaterialTheme.colorScheme.primary`
*   **Incorrect:** `Color(0xFF6200EE)`

### 2.1. Defining Colors
Define base "Seed" colors in `ui/theme/Color.kt`.

```kotlin
// ui/theme/Color.kt
val SeedPrimary = Color(0xFF006C4C) // Locus Green
val SeedSecondary = Color(0xFF4C6357)
val SeedTertiary = Color(0xFF3E6373)
```

## 3. Kotlin Coding Style

Follow the [Official Kotlin Style Guide](https://developer.android.com/kotlin/style-guide).

### 3.1. Specific Enforcement
*   **Immutability:** Use `val` by default. Use `var` only when necessary.
*   **Visibility:** Use `private` or `internal` by default. Only make `public` what is necessary for the API.
*   **Trailing Commas:** Enable trailing commas in formatting.

### 3.2. Compose Naming
*   **Composables:** PascalCase (Noun-phrase).
    *   `DashboardScreen`, `StatusCard`.
*   **Event Handlers:** `on[Event]` (lambda parameters).
    *   `onStopClick: () -> Unit`.

## 4. File Organization
*   **One Class Per File:** Unless the classes are small DTOs or sealed subclasses tightly coupled to the parent.
*   **Ordering:**
    1.  Constants (`const val`)
    2.  Properties
    3.  Initialization (`init`)
    4.  Public Functions
    5.  Private Functions
