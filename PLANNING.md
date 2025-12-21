# Architecture: User-Owned Precision Tracker

This document serves as an index for the Locus project documentation.

## Documentation Index

*   **[Core Philosophy](docs/technical_discovery/philosophy.md):** The guiding principles of data sovereignty and precision.
*   **[Infrastructure & Security](docs/technical_discovery/infrastructure.md):** AWS S3, Authentication, Encryption, and Cost Projections.
*   **[Android Architecture](docs/technical_discovery/android_architecture.md):** Details on the Android Client, Services, and Workers.
*   **[User Flows](docs/technical_discovery/user_flows.md):** The bootstrap, daily usage, and recovery journeys.
*   **[Functional Requirements](docs/technical_discovery/functional_requirements_index.md):** The detailed business rules and system behaviors.
*   **[Data Strategy](docs/technical_discovery/data_strategy.md):** Storage format, Schema Versioning, and JSON structure.
*   **[Deployment](docs/technical_discovery/deployment.md):** Building and distributing the application.

## Implementation Specifications
*   **[Data Persistence Spec (Phase 1)](docs/technical_discovery/specs/data_persistence_spec.md):** Room DB Schema and DAOs.

## High-Level Architecture

```mermaid
graph TD
    subgraph Android App
        UI[User Interface] -->|Setup Keys| Provisioner
        UI -->|View History| Visualizer
        Tracker[Tracker Service] -->|GPS Data| LocalDB[(Room DB)]
        Sync[Sync Worker] -->|Read Batch| LocalDB
        Sync -->|Delete Confirmed| LocalDB
    end

    subgraph AWS User Account
        Provisioner -->|Deploy Stack| CloudFormation
        CloudFormation -->|Create| S3Bucket[(S3 Bucket)]
        Sync -->|PutObject .gz| S3Bucket
        Visualizer -->|List/Get| S3Bucket
    end

    style S3Bucket fill:#f9f,stroke:#333,stroke-width:2px
    style LocalDB fill:#ccf,stroke:#333,stroke-width:2px
```
