# Pull Request Workflow

*   **Initial Synchronization:** Synchronize the working branch with the primary branch (main or master) before beginning any work.
*   **Final Synchronization:** Update the working branch with the latest changes from the primary branch immediately prior to creating the pull request.
*   **Verification:** Ensure all tests and checks pass locally after synchronizing with the primary branch.
*   **Merge Conflicts:** If asked to resolve merge conflicts, **do not** attempt to resolve them on the current branch. Instead:
    1.  **Stop and Update:** Check out the primary branch (`main` or `master`) and **pull the latest changes** to ensure it is 100% up to date.
    2.  Create a **new branch** from the updated primary branch.
    3.  **Redo the changes** on that new branch.
    4.  **Submit a new Pull Request** for this new branch.
    *   *Do not bother resolving merge conflicts on the existing branch.*
