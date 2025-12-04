# Navigation Architecture

**Parent:** [UI & Presentation Specification](../ui_presentation_spec.md)

The application uses a flat hierarchy with a **Bottom Navigation Bar** for standard devices and a **Navigation Rail** for large screens.

## 1. Navigation Graph

```mermaid
graph TD
    User((User)) --> Onboarding((Onboarding Flow))
    Onboarding -.-> |Success| Home[Dashboard / Home]

    subgraph "Main Navigation"
        Home --> Map[Map / History]
        Home --> Logs[Logs / Diagnostics]
        Home --> Settings[Settings]
    end

    Map --> Detail[Detail Bottom Sheet]
```

## 2. Layout & Components

### 2.1. Standard Layout (Phone)
*   **Component:** **Bottom Navigation Bar**.
*   **Visibility:** Persistent. It does **not** hide on scroll to ensure consistent navigation access.

### 2.2. Large Screen Layout (Tablet / Landscape > 600dp)
*   **Component:** **Navigation Rail** (Vertical Side Bar).
*   **Placement:** Anchored to the **Left** edge of the screen.
*   **Visibility:** Persistent.
*   **Adaptation:** The Bottom Navigation Bar must transform into the Navigation Rail on screens wider than `600dp`.

## 3. Top-Level Destinations

| Destination | Icon (Material Symbol) | Purpose |
| :--- | :--- | :--- |
| **Dashboard** | `dashboard` | Current status, immediate stats, manual actions. |
| **Map** | `map` | Historical data visualization and exploration. |
| **Logs** | `terminal` | Real-time diagnostic log stream and filters. |
| **Settings** | `settings` | Configuration, identity, and app-wide preferences. |

## 4. Back Handling & Routing

### 4.1. General
*   **Dashboard:** As the start destination, pressing the Back Button must background the application (standard Android Home behavior).
*   **Cross-Tab:** Switching tabs resets the back stack for that tab.

### 4.2. Map Screen (Deep Hierarchy)
The Map screen involves nested states (Bottom Sheet modes). The Back Button must follow this specific hierarchy:

1.  **Point Detail Mode:** If open, Back navigates to **Day Summary Mode** (Sheet Expanded/Peeking).
2.  **Expanded Bottom Sheet:** If expanded, Back collapses the sheet to its **Minimized (Peeking)** state.
3.  **Minimized Sheet:** If the sheet is minimized and the user is at the root map view, Back backgrounds the application.
