# Operational Documentation Backlog

This document lists operational topics that have been identified as necessary but are currently pending detailed documentation. These guides are intended to support the user in their role as the "System Administrator" of their Locus deployment.

## 1. Incident Response & Troubleshooting Guide
**Goal:** Empower the user to diagnose and resolve common issues without needing deep code knowledge.
**Scope:**
*   **Decision Tree:** "Tracking stopped" vs. "Uploads failing" vs. "App crashing."
*   **Log Analysis:** How to read and interpret the crash dumps and logs stored in the S3 `diagnostics/` folder.
*   **Recovery Procedures:** Step-by-step actions for "Soft Reset" (Clear Cache), "Hard Reset" (Clear Data), and "Infrastructure Reset" (Re-running CloudFormation).

## 2. Cost Management (FinOps)
**Goal:** Help the user monitor and optimize the AWS costs associated with their sovereign deployment.
**Scope:**
*   **Budgeting:** Instructions for setting up AWS Budgets and billing alarms.
*   **Cost Breakdown:** Explanation of costs per component (S3 Storage, S3 Requests, CloudWatch, Data Transfer).
*   **Optimization:** Strategies for reducing costs (e.g., adjusting S3 Lifecycle policies, bulk archiving old data to Glacier).

## 3. Data Operations (Manual Management)
**Goal:** Define procedures for managing data directly in S3, bypassing the mobile app interface.
**Scope:**
*   **Bulk Export:** How to download all tracks for a specific year/month using the AWS CLI.
*   **Manual Deletion:** Safe procedures for deleting specific history ranges (e.g., "Prune data older than 2020").
*   **Backup & Restore:** Strategies for backing up the S3 bucket to a local drive or another cloud provider ("Exit Strategy").

## 4. Security Operations (SecOps)
**Goal:** Define the ongoing security maintenance tasks required for a long-lived deployment.
**Scope:**
*   **Key Rotation:** Strategy and steps for rotating the IAM Access Keys used by the application (if necessary).
*   **Access Auditing:** How to use CloudTrail (if enabled) or S3 Access Logs to verify that only the user's device is accessing the data.
*   **Lost Device Protocol:** The immediate actions to take if the mobile device is lost (e.g., revoking the specific IAM User credentials).

## 5. Performance Analysis
**Goal:** Provide a guide for interpreting telemetry to understand system performance and battery impact.
**Scope:**
*   **Battery Drain Analysis:** How to correlate "Wake Lock" duration logs with battery usage.
*   **Network Efficiency:** Analyzing the ratio of "Successful Uploads" vs. "Failed/Retried Uploads."
*   **Latency Monitoring:** Understanding S3 put/get latencies from the device perspective.
