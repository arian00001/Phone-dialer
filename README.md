# Premium Glass Dialer

Native Android app starter written in Kotlin and Jetpack Compose. UI styling is white with lime-green accents and glass-like cards.

## Features included in source
- Phone keypad with editable number display and call launch through Android's cellular call system.
- `ACTION_DIAL` fallback when direct-call permission is unavailable.
- Contact search/list, create contact through Android's native contact editor, and open contact details in the system Contacts app (where edit/delete are available).
- Recent call log list, callback by tapping a recent entry, and permission/empty states.
- Android default-dialer role request and an `InCallService` with an in-call screen for answer, hang up, and hold/resume.
- GitHub Actions workflow to build and upload a debug APK artifact.

## Build APK with GitHub Actions
1. Extract the ZIP and push the *contents* of this folder to the root of a GitHub repository. Do not upload only the ZIP file.
2. Keep `.github/workflows/android-apk.yml` in the repository.
3. Open **Actions** → **Build Premium Glass Dialer APK** → **Run workflow**.
4. When the run succeeds, open the run and download `PremiumGlassDialer-debug-apk` under **Artifacts**.
5. A push to `main` or `master` that changes app/build/workflow files also starts a build.

The workflow uses JDK 17, Android SDK setup, Gradle 8.9, and `:app:assembleDebug`. Debug APKs are automatically signed with the debug key and do not need repository signing secrets. They are for testing, not a Play Store release.

## Android permissions and limitations
- Calls use the SIM and carrier service configured on the device. `CALL_PHONE` is requested for direct calling; without it, the app opens Android's system dialer.
- Contacts and call history require runtime permissions. Android or the device manufacturer may restrict call-log access unless this app is the selected default dialer.
- Default dialer role requires explicit user approval. Telecom behavior and showing the in-call screen can vary by Android version and manufacturer; test incoming/outgoing calls on the target phone before relying on this app as the everyday phone app.
- Dual-SIM selection is device/vendor dependent. Android may show its own SIM selection UI; this app does not promise a custom SIM picker on every phone.
- This project has been statically inspected in this environment, but an Android Gradle build and real-device SIM test could not be run here because the environment has no network access to fetch Gradle/Android dependencies. The GitHub Actions workflow performs the actual compile and will report any build failure.
