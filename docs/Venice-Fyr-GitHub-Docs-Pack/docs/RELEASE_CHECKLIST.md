# Release Checklist

This is a review checklist, not proof that release automation/signing already exists.

## Source and version

- [ ] release commit identified
- [ ] working tree clean
- [ ] versionCode/versionName reviewed
- [ ] changelog updated
- [ ] parity matrix reflects shipped state
- [ ] README does not claim unfinished features

## Tests/build

- [ ] `./gradlew test`
- [ ] `./gradlew lint`
- [ ] `./gradlew :app:assembleDebug`
- [ ] `./gradlew :venice-sdk:assembleRelease`
- [ ] relevant instrumentation tests
- [ ] Room migrations tested
- [ ] profile isolation tested
- [ ] streaming cancellation/error paths tested

## Security/privacy

- [ ] no committed secrets
- [ ] release manifest permissions reviewed
- [ ] exported components reviewed
- [ ] API-key persistence verified
- [ ] sensitive logging reviewed
- [ ] network security config reviewed
- [ ] privacy documentation matches implementation
- [ ] third-party SDK telemetry/data collection reviewed

## Legal

- [ ] repository license confirmed
- [ ] dependency licenses reviewed
- [ ] required NOTICE/attribution generated
- [ ] trademarks/branding reviewed
- [ ] provider terms reviewed for release behavior

## Signing/distribution

- [ ] signing configuration uses secure secret handling
- [ ] no signing keys committed
- [ ] release artifact reproduced/verified
- [ ] APK/AAB inspected
- [ ] AAR consumer rules/API surface reviewed
- [ ] checksums generated if distributed directly

## Documentation

- [ ] installation instructions verified from clean environment
- [ ] support/security links valid
- [ ] release notes written
- [ ] screenshots/assets match shipped UI
