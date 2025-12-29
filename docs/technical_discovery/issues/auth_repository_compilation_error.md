# AuthRepository Compilation Error

## Problem Description
During the implementation of `AuthRepositoryImpl`, a persistent Kotlin compiler error occurs when invoking AWS SDK for Kotlin methods. This prevents the module `:core:data` from compiling.

**Error Message:**
```
e: file:///app/core/data/src/main/kotlin/com/locus/core/data/repository/AuthRepositoryImpl.kt:83:66 Unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
public operator fun <T, R> DeepRecursiveFunction<TypeVariable(T), TypeVariable(R)>.invoke(value: TypeVariable(T)): TypeVariable(R) defined in kotlin
```

**Affected Methods:**
- `S3Client.headBucket`
- `S3Client.getBucketTagging`
- `CloudFormationClient.describeStacks`

## Attempted Fixes

### 1. Dependency Updates
- **Initial Version:** `aws.sdk.kotlin:s3:1.0.42`
- **Action:** Updated `libs.versions.toml` to use version `1.3.30` for all AWS SDK modules (`s3`, `cloudformation`, `sts`).
- **Result:** Error persists.

### 2. Syntax Variations
We attempted multiple syntactic approaches to rule out DSL resolution issues.

**Approach A: DSL Syntax**
```kotlin
client.headBucket {
    bucket = bucketName
}
```
*Result:* Compilation fails with `Unresolved reference`.

**Approach B: Explicit Request Object**
```kotlin
val request = HeadBucketRequest {
    bucket = bucketName
}
client.headBucket(request)
```
*Result:* Compilation fails with `Unresolved reference`.

### 3. Scope Isolation
We refactored the code to remove nested `use` blocks and ensure the `client` variable was explicitly typed and not shadowed.

```kotlin
val client: S3Client = awsClientFactory.createBootstrapS3Client(creds)
client.use { s3 ->
    s3.headBucket(...)
}
```
*Result:* Error persists.

### 4. Import Management
- Verified imports.
- Switched to Star Imports (`aws.sdk.kotlin.services.s3.*`) to ensure all extension functions and types were available.
- *Result:* Error persists.

### 5. Clean Builds
- Ran `./gradlew clean` multiple times.
- Deleted previously created wrapper classes (`S3Client.kt`, `CloudFormationClient.kt`) that might have caused naming conflicts.

## Hypothesis
The error `DeepRecursiveFunction...invoke` suggests a deep internal compiler confusion, possibly related to:
1.  **Coroutines/Suspend Mismatch:** The AWS SDK methods are `suspend`. The call sites are within `suspend` functions, but the error mentions `invoke` on a `TypeVariable`, which often happens when the compiler cannot infer the receiver of a lambda or function call in a complex coroutine context.
2.  **Compiler Bug:** The interaction between Kotlin 1.9.22 and the AWS SDK suspend functions might be triggering a specific compiler bug.
3.  **Classpath/Shadowing:** There might still be a phantom dependency or a class with the same name confusing the resolution, although `ls` checks suggests clean directories.

## Next Steps (Recommended)
1.  **Isolate:** Create a completely separate, minimal Gradle module or a single file reproduction script outside of the current `AuthRepositoryImpl` structure to test *only* the AWS SDK call.
2.  **Downgrade:** Try an older, known-good combination of Kotlin and AWS SDK if the "latest" is unstable.
3.  **Community:** Search for `DeepRecursiveFunction invoke receiver type mismatch` in relation to AWS SDK Kotlin.
