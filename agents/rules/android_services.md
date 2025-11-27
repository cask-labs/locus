# Android Services Rules

*   **Foreground Services:** Use Foreground Services for long-running operations that users must be aware of, such as location tracking.
*   **Battery Safety:** Implement a strict battery safety protocol that automatically stops intensive background tasks when the battery level drops below 10%.
*   **Notification Channels:** Categorize notifications into appropriate channels to give users control over interruptions.
*   **WorkManager:** Utilize WorkManager for deferrable and guaranteed background tasks.
*   **Resource Management:** Release resources such as location listeners and sensors immediately when they are no longer needed.
