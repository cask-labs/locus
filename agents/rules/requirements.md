# Requirements

*   **Technology Agnostic:** Write requirements that define *what* the system must do, not *how* it does it. Abstract away specific technologies (e.g., "cloud storage" instead of "AWS S3", "mobile device" instead of "Android").
*   **EARS Format:** Use the Easy Approach to Requirements Syntax (EARS) for all requirements.
    *   **Ubiquitous:** The <system> shall <response>.
    *   **Event-driven:** When <trigger>, the <system> shall <response>.
    *   **State-driven:** While <state>, the <system> shall <response>.
    *   **Unwanted behavior:** If <trigger>, then the <system> shall <response>.
    *   **Optional feature:** Where <feature> is included, the <system> shall <response>.
*   **Structure:** Present requirements as a flat list.
