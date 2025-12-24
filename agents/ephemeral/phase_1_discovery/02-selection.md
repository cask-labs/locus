# Architecture Selection: Phase 1 - Onboarding & Identity

## Comparison Matrix

| Dimension | Path A (ViewModel) | Path B (Service + Repo) | Path C (WorkManager) |
|-----------|--------------------|-------------------------|----------------------|
| **Resilience (R1.600)** | Low | **High** | High |
| **Security (Keys)** | High | **High** | Low (Serialization Risk) |
| **Real-time UX** | Instant | **Instant** | Delayed |
| **Complexity** | Low | **Medium** | Medium |
| **State Recovery** | Poor | **Excellent** | Good |

---

## Selected Path

**Picked:** **Path B: Domain State Machine + Foreground Service**

**Why:**
1.  **Resilience is Critical:** Provisioning CloudFormation stacks can take 2-5 minutes. The application must guarantee execution even if the user switches apps or the screen turns off. A Foreground Service with a persistent notification is the standard Android pattern for this.
2.  **Security:** We can keep the sensitive "Bootstrap Keys" in memory (within the Service/Repository scope) and pass them directly to the AWS SDK, avoiding the need to serialize them to disk as `WorkManager` `Data` would require.
3.  **Tombstoning:** By persisting the detailed "Provisioning Log" to a local file/db via the Repository, we ensure that if the app *does* crash, the user sees the exact error upon relaunch (The "Setup Trap"), rather than a generic "Something went wrong".

**Risks we're accepting:**
- **Service Complexity:** We must carefully manage the Service lifecycle (starting, stopping, binding) and ensure it plays nicely with Android 14+ restrictions.
- **State Synchronization:** The UI must reactively observe the Repository state, not the Service directly, to decouple the View from the Process.

**Deferred Choices:**
- **Admin Provisioning:** We will build the engine for Standard users first. Admin templates are supported by the architecture but won't be exposed in the UI yet.
