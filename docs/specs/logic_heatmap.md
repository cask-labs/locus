# Heatmap Logic Specification

**Parent:** [UI & Presentation Specification](ui_presentation_spec.md)
**Related:** [Visualization Requirements](../requirements/visualization.md)

This document defines the mathematical and rendering logic for the Signal Quality Heatmap overlay on the Map screen.

## 1. Concept
The heatmap visualizes the network signal quality (Cellular or WiFi) along the recorded track. Instead of a separate spatial layer, it uses **Path Colorization**, where the track line itself changes color based on the signal strength recorded at each point.

## 2. Rendering Logic
### 2.1. Color Mapping
Each point is assigned a color based on its signal strength (`dBm`) and type (`Cellular` vs `WiFi`).

**Thresholds:**
| Quality | Color (Hex/Name) | Cellular (dBm) | WiFi (dBm) |
| :--- | :--- | :--- | :--- |
| **High** | Green (`#4CAF50`) | > -85 | > -60 |
| **Medium** | Yellow (`#FFC107`) | -85 to -105 | -60 to -80 |
| **Low** | Red (`#F44336`) | < -105 | < -80 |

*   **Style Difference:**
    *   **Cellular:** Solid Line.
    *   **WiFi:** Dashed Line (or distinct stroke pattern) to differentiate source.

### 2.2. Smoothing Algorithm (Sliding Window)
To prevent the path color from flickering rapidly due to transient signal noise, the system applies a **Simple Moving Average (SMA)** smoothing algorithm before rendering.

**Algorithm:**
For each point $P_i$ in the track:
1.  Identify a window of size $N$ (e.g., **N=3** points: current, previous, and next).
2.  Calculate the average signal strength ($\overline{S}$) of the valid signal readings in that window.
3.  Map $\overline{S}$ to the thresholds defined above to determine the color of the segment following $P_i$.

**Constraints:**
*   **Gap Handling:** If a visual discontinuity (Gap > 5 mins) exists within the window, the window **truncates** at the gap. Do not average values across a track break.
*   **Missing Data:** Points with no signal data are excluded from the average. If the result is "No Data", that segment is drawn in the default path color (or gray) if necessary, but typically the "No Data" gaps are just standard track lines.

### 2.3. Visual Discontinuity
The heatmap respects the same visual discontinuity rules as the standard track:
*   If time gap > 5 minutes, **no line is drawn**.
*   The heatmap does *not* interpolate color across empty space.

## 3. Implementation Note
*   This logic should be applied at the **Presentation Layer** (ViewModel or Transformer) when preparing the `Polyline` or `Path` object for the map.
*   The raw data in the database remains untouched; this is purely for visualization.
