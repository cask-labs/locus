# Data Collection & Tracking Requirements

## 1.1. Location Recording
*   **Precision:** The system shall record geospatial location data (Latitude, Longitude, Altitude) at a standard frequency of 10 seconds.
*   **Batching:** The system shall utilize hardware batching (max latency 2 minutes) to allow the CPU to sleep between updates while maintaining the 10-second recording interval.
*   **Strategy:** The system shall utilize the Android Native `LocationManager` for all flavors to ensure consistent behavior.
*   **Persistence:** While the application is in the background or the device is sleeping, the system shall maintain continuous data collection via batched updates.
*   **Battery Saver Override:** While the OS "Battery Saver" mode is active, the system shall continue standard data collection operations.
*   **Network Quality:** When recording a location point, the system shall record the network quality metrics (Signal Level 0-4 and Raw dBm) for both Cellular and WiFi interfaces simultaneously, if available.

## 1.2. Optimization & Sensor Fusion
*   **Dynamic Sampling:** When the device speed exceeds 4.5 m/s, the system shall record auxiliary environmental sensors (accelerometer, magnetometer, barometer).
*   **Stationary Mode:** When no movement is detected for 2 minutes, the system shall suspend GPS acquisition.
*   **Wake-on-Motion:** While in Stationary Mode, the system shall prioritize hardware-backed Significant Motion triggers to resume GPS acquisition, falling back to periodic accelerometer polling only if necessary.

## 1.3. Battery Safety Protocol
*   **Low Battery (< 10%):** While the battery capacity is less than 10%, the system shall reduce the recording frequency to 30 seconds.
*   **Low Battery Uploads:** While the battery capacity is less than 10%, the system shall pause automatic data uploads.
*   **Critical Battery (< 3%):** While the battery capacity is less than 3%, the system shall enter "Deep Sleep" mode: GPS acquisition shall stop, and all WakeLocks shall be released.
*   **Recovery:** When the battery capacity rises above 15%, the system shall resume standard recording and upload schedules.
