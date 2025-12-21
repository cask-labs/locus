# Design System & Accessibility

**Parent:** [UI & Presentation Specification](../ui_presentation_spec.md)

## 1. Design System & Philosophy

### 1.1. Visual Style
*   **Framework:** **Material Design 3 (Material You)**. The app must leverage dynamic coloring based on the user's system wallpaper to feel native and integrated.
*   **Theme:** Fully supported **Light** and **Dark** modes, respecting the system-wide setting by default.
*   **Typography:** Use the standard Material Type Scale (Headline, Title, Body, Label) with support for Dynamic Type (system font scaling).
*   **Iconography:** Filled icons for active states, outlined icons for inactive states (Material Symbols).
*   **Splash Screen:** The application must implement the **Android 12+ SplashScreen API** to provide a seamless launch experience, displaying the Locus logo on a system-adaptive background.

### 1.2. Philosophy: "Subtle by Default"
*   **Notification:** The Persistent Notification is the primary status indicator outside the app. It must never beep or vibrate unless a **Fatal Error** occurs.
*   **In-App:** Transient errors (e.g., "Network Timeout") are displayed as unobtrusive Snackbars or inline status text, never modal dialogs.
*   **Transparency:** The UI must always answer "What is the app doing right now?" (e.g., Recording, Syncing, Idle).

## 2. Accessibility (A11y) Requirements
*   **Touch Targets:** All interactive elements (FABs, Calendar dates) must be at least **48x48dp**.
*   **Content Descriptions:** Mandatory for all icons (e.g., "Signal Strength: Good", "Battery: Charging").
*   **Color Blindness:** The Signal Heatmap must use colors distinguishing enough for common vision deficiencies, or offer a pattern alternative (e.g., dashed lines).
*   **Scale:** Layouts must adapt to System Font Scale up to 200%.
