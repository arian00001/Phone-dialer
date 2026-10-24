# Build / Source Scan Report

Date: 2026-10-24

## CI workflow change
- Removed the `android-actions/setup-android@v3` step that was failing before Gradle started.
- Removed all `sdkmanager` invocations, including any possible attempt to resolve the deprecated `tools` package.
- Pinned the runner to `ubuntu-24.04` and uses the Android SDK preinstalled on GitHub-hosted Ubuntu runners.
- Added explicit checks for `platform-tools`, `platforms/android-35`, and `build-tools/35.0.0` before invoking Gradle.
- Kept JDK 17, Gradle 8.9, `:app:assembleDebug`, APK artifact upload, and added a diagnostics artifact on failure.

## Checks performed in this environment
- ZIP archive integrity checked.
- AndroidManifest.xml parsed as XML.
- Kotlin source files passed a delimiter-balance scan.
- Workflow structure and required project paths checked statically.

## Not verified here
- A GitHub Actions hosted-runner execution was not available from this environment.
- The Gradle Android build and physical-device SIM calling tests have not been run here.
- This ZIP therefore fixes the reported workflow design issue by bypassing the failing SDK setup action, but it cannot be honestly described as a fully verified error-free APK until a GitHub Actions run succeeds.

## If the workflow fails again
Open the failed Actions run and inspect the first failing step. If the hosted image does not include one of the asserted Android SDK package directories, use the runner's current preinstalled SDK package list and adjust the SDK version deliberately rather than adding the deprecated `tools` package.
