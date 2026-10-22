# Premium Glass Dialer

A native Kotlin + Jetpack Compose Android dialer starter project with a white / lime glass-inspired UI.

## Included
- Number keypad and editable number display
- Call launch through Android's real phone call system (`ACTION_CALL`) after permission
- SIM chooser intent for devices that expose one
- Contacts list, contact search, create contact form (opens native contact editor)
- Recent call log list (requires call-log permission)
- Default phone app role request entry point
- Runtime permission requests and empty/error states

## Build
1. Open this folder in Android Studio (Giraffe or newer; current stable recommended).
2. Let Gradle sync.
3. Build > Build APK(s).

## Important Android limitations
- Android/phone vendor policies vary. To place a call directly, grant `CALL_PHONE`; otherwise the app opens the system dialer.
- Call history requires the user to grant `READ_CALL_LOG`. Google Play has additional policy restrictions for call-log permissions.
- Becoming the default dialer requires explicit user approval and a complete dialer implementation. This starter requests the role but is not a full replacement for the system's in-call UI; incoming/in-call UI must be implemented and tested separately before using it as the everyday default phone app.
- Emergency calls should always be made through the system's supported emergency-call flow.
- No app can guarantee SIM selection on every device; dual-SIM behavior is manufacturer-dependent.
