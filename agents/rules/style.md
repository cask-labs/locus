# Style & Resource Rules

*   **Resource Naming:** Use `snake_case` for all XML resources.
*   **Feature Prefixes:** Prefix string, layout, and ID resources with the feature name to prevent collisions and improve discoverability.
*   **Layout Naming:** Prefix layout files with their type: `activity_`, `fragment_`, `item_`, or `view_`.
*   **Color Usage:** Use `MaterialTheme.colorScheme` tokens in UI code instead of hardcoded hex values or standard Color constants.
*   **Color Definitions:** Define base seed colors only in the designated theme file.
*   **Drawable Naming:** Prefix drawables with `ic_` (icons), `bg_` (backgrounds), or `img_` (images).
