# Infrastructure & Security

*   **Storage:** AWS S3 (User Owned).
*   **Authentication:**
    *   **Setup:** User creates IAM User with "S3 Only" policy.
    *   **Runtime:** App uses these keys to check/create its own bucket.
*   **Encryption:** Standard AWS S3 Server-Side Encryption (SSE-S3).
*   **Monitoring:** Firebase Crashlytics & Analytics.

# Cost Projections

*   **S3 Storage:** <$0.10/year (Compressed).
*   **Request Costs:** ~$0.02/month (Batched).
