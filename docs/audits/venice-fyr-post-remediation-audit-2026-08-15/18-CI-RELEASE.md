# CI and Release

`.github/workflows/android-ci.yml` is real and runs on pushes/PRs to main. It validates the wrapper, uses JDK 17, runs unit tests and lint, builds the debug APK and release AAR, uploads them, then builds an unsigned release APK in a dependent job.

Missing by design/current scope: device tests, API fixture drift checks, SDK binary compatibility, dependency vulnerability/license checks, signed artifact provenance, and installed/published release smoke. Public release remains blocked by signing/distribution setup and the character-asset license gate. Local release gates are recorded in `22-VALIDATION-RESULTS.md`.
