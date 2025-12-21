# Behavioral Specification: Adaptive Battery Safety

**Bounded Context:** This specification acts as a "Governor" layer, overriding the standard behaviors of Tracking and Synchronization based on the device's battery resource state.

**Prerequisite:** None (Logic layer).
**Downstream:** Governs **[Intelligent Tracking](02_intelligent_tracking.md)** and **[Cloud Synchronization](03_cloud_synchronization.md)**.

---

## Low Battery State (< 10%)
*   **R4.100** **While** the battery capacity is less than 10% (and greater than 3%), the system **shall** enter "Low Power Mode".
*   **R4.200** **While** in Low Power Mode, the system **shall** reduce the location recording frequency to a fixed interval of 30 seconds.
*   **R4.300** **While** in Low Power Mode, the system **shall** pause all automatic background synchronization jobs.
*   **R4.400** **When** a Manual Sync is requested during Low Power Mode, the system **shall** permit the upload.

## Critical Battery State (< 3%)
*   **R4.500** **While** the battery capacity is less than 3%, the system **shall** enter "Deep Sleep Mode".
*   **R4.600** **While** in Deep Sleep Mode, the system **shall** cease all location acquisition.
*   **R4.700** **While** in Deep Sleep Mode, the system **shall** release all processor locks to allow the CPU to sleep.
*   **R4.800** **When** a Manual Sync is requested during Deep Sleep Mode, the system **shall** reject the request and display a "Battery Critical" error.

## Recovery Logic
*   **R4.900** **When** the battery capacity rises above 15% (Hysteresis Threshold), the system **shall** exit Low Power/Deep Sleep modes and resume standard recording and upload schedules.
*   **R4.1000** **When** the device is connected to an external power source (Charging), the system **shall** immediately exit any power-saving modes and resume standard operation, regardless of the percentage level.
