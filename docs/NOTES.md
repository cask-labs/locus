# Notes

*   Consider letting the visualisation choose which bucket to view from in the account and to restrict this feature conditionally on the bootstrap step
*   Add documentation on how to build the APK by cloning the repo, ideally handled by a script to simplify the process
*   Address concurrency issues when multiple devices write to the same bucket to prevent conflicting location data (e.g., user appearing in two places at once)
*   Implement cache eviction policies to prevent excessive storage usage by downloaded track data
*   Verify the IAM strategy against AWS documentation to ensure it functions as intended and adheres to best practices
*   Update all documentation to reflect the approach that avoids using CloudFormation for creating IAM credentials
