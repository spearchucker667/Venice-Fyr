# 02-FILE-AUDIT-LEDGER.md

Full census of all tracked files in the Venice Fyr Android repository.

- Generated: 2026-08-15T22:37:37Z (UTC)
- Repository HEAD: `1da314241b0ffcd622dcf2732940e2df432172c7`
- Total tracked files: 321
- Source-of-truth mirror: `.source/venice-api-docs/` (read-only; not a write target)
- Desktop parity mirror: `.source/Venice_Forge-desktop/` (read-only; not a write target)
- `docs/Venice-Fyr-GitHub-Docs-Pack/` is a packaged mirror of GitHub-facing docs.

| Area | File | Lines | Reviewed (this audit) | Notes |
|------|------|-------|------------------------|-------|
| REPO GOVERNANCE | `.gitattributes` | 2 | N |  |
| CI/CD | `.github/CODEOWNERS` | 4 | N |  |
| CI/CD | `.github/ISSUE_TEMPLATE/bug_report.yml` | 85 | N |  |
| CI/CD | `.github/ISSUE_TEMPLATE/config.yml` | 8 | N |  |
| CI/CD | `.github/ISSUE_TEMPLATE/documentation.yml` | 27 | N |  |
| CI/CD | `.github/ISSUE_TEMPLATE/feature_request.yml` | 50 | N |  |
| CI/CD | `.github/PULL_REQUEST_TEMPLATE.md` | 57 | N |  |
| REPO GOVERNANCE | `.gitignore` | 19 | Y |  |
| REPO GOVERNANCE | `AGENTS.md` | 138 | Y |  |
| REPO GOVERNANCE | `ANDROID_PORT_HANDOFF.md` | 233 | N |  |
| REPO GOVERNANCE | `CHANGELOG.md` | 36 | N |  |
| REPO GOVERNANCE | `CODE_OF_CONDUCT.md` | 33 | N |  |
| REPO GOVERNANCE | `CONTRIBUTING.md` | 168 | N |  |
| REPO GOVERNANCE | `LEGAL.md` | 88 | N |  |
| REPO GOVERNANCE | `LICENSE` | 201 | N |  |
| REPO GOVERNANCE | `NOTICE` | 22 | N |  |
| REPO GOVERNANCE | `PRIVACY.md` | 100 | N |  |
| REPO GOVERNANCE | `README.md` | 289 | N |  |
| REPO GOVERNANCE | `SECURITY.md` | 81 | N |  |
| REPO GOVERNANCE | `SOURCE_BASELINE.md` | 57 | Y |  |
| REPO GOVERNANCE | `SUPPORT.md` | 59 | N |  |
| APP | `app/build.gradle.kts` | 68 | N |  |
| APP | `app/src/main/AndroidManifest.xml` | 23 | N |  |
| APP | `app/src/main/java/io/github/spearchucker667/veniceforge/android/MainActivity.kt` | 17 | N |  |
| APP | `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt` | 266 | N |  |
| APP | `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatScreen.kt` | 123 | N |  |
| APP | `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt` | 200 | N |  |
| APP | `app/src/main/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalog.kt` | 48 | N |  |
| APP | `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageScreen.kt` | 200 | N |  |
| APP | `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt` | 122 | N |  |
| APP | `app/src/main/java/io/github/spearchucker667/veniceforge/android/ui/ConfigScreen.kt` | 173 | N |  |
| APP | `app/src/main/res/drawable/ic_launcher_background.xml` | 7 | N |  |
| APP | `app/src/main/res/drawable/ic_launcher_foreground.xml` | 15 | N |  |
| APP | `app/src/main/res/drawable/ic_launcher_monochrome.xml` | 12 | N |  |
| APP | `app/src/main/res/drawable/ic_venice_keys_deep_blue.xml` | 9 | N |  |
| APP | `app/src/main/res/drawable/ic_venice_keys_off_white.xml` | 9 | N |  |
| APP | `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | 6 | N |  |
| APP | `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | 6 | N |  |
| APP | `app/src/main/res/mipmap-hdpi/ic_launcher.png` | binary | N |  |
| APP | `app/src/main/res/mipmap-hdpi/ic_launcher_round.png` | binary | N |  |
| APP | `app/src/main/res/mipmap-mdpi/ic_launcher.png` | binary | N |  |
| APP | `app/src/main/res/mipmap-mdpi/ic_launcher_round.png` | binary | N |  |
| APP | `app/src/main/res/mipmap-xhdpi/ic_launcher.png` | binary | N |  |
| APP | `app/src/main/res/mipmap-xhdpi/ic_launcher_round.png` | binary | N |  |
| APP | `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` | binary | N |  |
| APP | `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png` | binary | N |  |
| APP | `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` | binary | N |  |
| APP | `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png` | binary | N |  |
| APP | `app/src/main/res/values-v27/themes.xml` | 5 | N |  |
| APP | `app/src/main/res/values/strings.xml` | 14 | N |  |
| APP | `app/src/main/res/values/themes.xml` | 8 | N |  |
| APP | `app/src/main/res/xml/network_security_config.xml` | 3 | N |  |
| APP | `app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt` | 246 | N |  |
| APP | `app/src/test/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalogTest.kt` | 12 | N |  |
| APP | `app/src/test/resources/robolectric.properties` | 1 | N |  |
| BUILD/TOOLING | `build.gradle.kts` | 5 | N |  |
| CORE:COMMON | `core/common/build.gradle.kts` | 17 | N |  |
| CORE:COMMON | `core/common/src/main/AndroidManifest.xml` | 1 | N |  |
| CORE:COMMON | `core/common/src/main/java/io/github/spearchucker667/veniceforge/core/common/Redactor.kt` | 17 | N |  |
| CORE:COMMON | `core/common/src/test/java/io/github/spearchucker667/veniceforge/core/common/RedactorTest.kt` | 15 | N |  |
| CORE:DATA | `core/data/build.gradle.kts` | 56 | N |  |
| CORE:DATA | `core/data/schemas/io.github.spearchucker667.veniceforge.core.data.AppDatabase/1.json` | 478 | N |  |
| CORE:DATA | `core/data/src/main/AndroidManifest.xml` | 2 | N |  |
| CORE:DATA | `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt` | 47 | N |  |
| CORE:DATA | `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/Converters.kt` | 21 | N |  |
| CORE:DATA | `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/DataServices.kt` | 22 | N |  |
| CORE:DATA | `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ConversationDao.kt` | 33 | N |  |
| CORE:DATA | `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageDao.kt` | 31 | N |  |
| CORE:DATA | `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageToolCallDao.kt` | 17 | N |  |
| CORE:DATA | `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ProfileDao.kt` | 30 | N |  |
| CORE:DATA | `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ConversationEntity.kt` | 43 | N |  |
| CORE:DATA | `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ConversationFolderEntity.kt` | 27 | N |  |
| CORE:DATA | `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageEntity.kt` | 38 | N |  |
| CORE:DATA | `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageToolCallEntity.kt` | 35 | N |  |
| CORE:DATA | `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ProfileEntity.kt` | 14 | N |  |
| CORE:DATA | `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt` | 83 | N |  |
| CORE:DATA | `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ProfileRepository.kt` | 27 | N |  |
| CORE:DATA | `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ChatRepositoryTest.kt` | 59 | N |  |
| CORE:DATA | `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/MigrationTest.kt` | 51 | N |  |
| CORE:DATA | `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ProfileIsolationTest.kt` | 67 | N |  |
| CORE:DATA | `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ProfileRepositoryTest.kt` | 38 | N |  |
| CORE:DATA | `core/data/src/test/resources/robolectric.properties` | 1 | N |  |
| CORE:DESIGNSYSTEM | `core/designsystem/build.gradle.kts` | 24 | N |  |
| CORE:DESIGNSYSTEM | `core/designsystem/src/main/AndroidManifest.xml` | 1 | N |  |
| CORE:DESIGNSYSTEM | `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/CodexPet.kt` | 128 | N |  |
| CORE:DESIGNSYSTEM | `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceColors.kt` | 93 | N |  |
| CORE:DESIGNSYSTEM | `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceForgeTheme.kt` | 26 | N |  |
| CORE:DESIGNSYSTEM | `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceLoadingIndicator.kt` | 86 | N |  |
| CORE:DESIGNSYSTEM | `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceTypography.kt` | 129 | N |  |
| CORE:DESIGNSYSTEM | `core/designsystem/src/main/res/drawable-nodpi/ayanami_rei_spritesheet.webp` | binary | N |  |
| CORE:SECURITY | `core/security/build.gradle.kts` | 18 | N |  |
| CORE:SECURITY | `core/security/src/main/AndroidManifest.xml` | 1 | N |  |
| CORE:SECURITY | `core/security/src/main/java/io/github/spearchucker667/veniceforge/core/security/SecureSecretStore.kt` | 100 | N |  |
| DOCUMENTATION | `docs/API_INTEGRATION_GUIDE.md` | 86 | N |  |
| DOCUMENTATION | `docs/BRANDING.md` | 136 | N |  |
| DOCUMENTATION | `docs/DESKTOP_SOURCE_BOOTSTRAP.md` | 286 | N |  |
| DOCUMENTATION | `docs/DEVELOPMENT_GUIDE.md` | 140 | N |  |
| DOCUMENTATION | `docs/ELECTRON_TO_ANDROID_MAP.md` | 35 | N |  |
| DOCUMENTATION | `docs/FEATURE_PARITY_MATRIX.md` | 43 | N |  |
| DOCUMENTATION | `docs/GETTING_STARTED.md` | 130 | N |  |
| DOCUMENTATION | `docs/GITHUB_DOCS_INDEX.md` | 54 | N |  |
| DOCUMENTATION | `docs/PROVIDER_PARITY.md` | 34 | N |  |
| DOCUMENTATION | `docs/RELEASE_CHECKLIST.md` | 58 | N |  |
| DOCUMENTATION | `docs/SDK_EXAMPLES.md` | 109 | N |  |
| DOCUMENTATION | `docs/SDK_GUIDE.md` | 79 | N |  |
| DOCUMENTATION | `docs/SECURITY_AND_STORAGE_CONTRACT.md` | 15 | N |  |
| DOCUMENTATION | `docs/TROUBLESHOOTING.md` | 148 | N |  |
| DOCUMENTATION | `docs/USER_GUIDE.md` | 59 | N |  |
| DOCUMENTATION | `docs/VENICE_API_PORT_MATRIX.md` | 59 | N |  |
| DOCUMENTATION | `docs/VENICE_API_SOURCE_BOOTSTRAP.md` | 88 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/.github/CODEOWNERS` | 4 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/.github/ISSUE_TEMPLATE/bug_report.yml` | 85 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/.github/ISSUE_TEMPLATE/config.yml` | 8 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/.github/ISSUE_TEMPLATE/documentation.yml` | 27 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/.github/ISSUE_TEMPLATE/feature_request.yml` | 50 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/.github/PULL_REQUEST_TEMPLATE.md` | 57 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/AGENT_HANDOFF_REVIEW_FIRST.md` | 367 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/CHANGELOG.md` | 32 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/CODE_OF_CONDUCT.md` | 33 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/CONTRIBUTING.md` | 168 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/LEGAL.md` | 88 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/LICENSE` | 201 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/NOTICE` | 22 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/PACKAGE_MANIFEST.md` | 74 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/PRIVACY.md` | 100 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/README.md` | 184 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/SECURITY.md` | 81 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/SUPPORT.md` | 59 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/docs/BRANDING.md` | 133 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/docs/DEVELOPMENT_GUIDE.md` | 140 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/docs/GETTING_STARTED.md` | 130 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/docs/GITHUB_DOCS_INDEX.md` | 54 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/docs/RELEASE_CHECKLIST.md` | 58 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/docs/SDK_GUIDE.md` | 79 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/docs/TROUBLESHOOTING.md` | 148 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/docs/USER_GUIDE.md` | 59 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/docs/assets/venice-fyr-banner.png` | binary | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/docs/assets/venice-fyr-banner.svg` | 89 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/docs/assets/venice-fyr-mark.png` | binary | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/docs/assets/venice-fyr-mark.svg` | 53 | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/docs/assets/venice-fyr-social-preview.png` | binary | N |  |
| DOCUMENTATION | `docs/Venice-Fyr-GitHub-Docs-Pack/package-manifest.json` | 45 | N |  |
| DOCUMENTATION | `docs/assets/AGENT_HANDOFF_REVIEW_FIRST.md` | 367 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/AGENT_HANDOFF_GEMINI_3_7_FLASH.md` | 288 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/MANIFEST.json` | 47 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/PREVIEW.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/README.md` | 38 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/SHA256SUMS.txt` | 78 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/SOURCE_ATTRIBUTION.md` | 23 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/SOURCE_INTEGRITY.json` | 9 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/drawable/ic_launcher_background.xml` | 7 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/drawable/ic_launcher_foreground.xml` | 15 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/drawable/ic_launcher_monochrome.xml` | 12 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/drawable/ic_venice_keys_deep_blue.xml` | 9 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/drawable/ic_venice_keys_off_white.xml` | 9 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/mipmap-anydpi-v26/ic_launcher.xml` | 6 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/mipmap-anydpi-v26/ic_launcher_round.xml` | 6 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/mipmap-hdpi/ic_launcher.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/mipmap-hdpi/ic_launcher_round.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/mipmap-mdpi/ic_launcher.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/mipmap-mdpi/ic_launcher_round.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/mipmap-xhdpi/ic_launcher.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/mipmap-xhdpi/ic_launcher_round.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/mipmap-xxhdpi/ic_launcher.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/mipmap-xxhdpi/ic_launcher_round.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/mipmap-xxxhdpi/ic_launcher.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/android/res/mipmap-xxxhdpi/ic_launcher_round.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/docs/github/venice-fyr-social-preview-v2-1280x640.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/docs/readme/venice-fyr-banner-v2-1600x480.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/in-app/splash/venice-fyr-splash-dark-1080x2400.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/in-app/splash/venice-fyr-splash-light-1080x2400.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/on-deep-blue/venice-keys-1024.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/on-deep-blue/venice-keys-256.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/on-deep-blue/venice-keys-512.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/on-midnight-blue/venice-keys-1024.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/on-midnight-blue/venice-keys-256.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/on-midnight-blue/venice-keys-512.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/on-off-white/venice-keys-1024.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/on-off-white/venice-keys-256.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/on-off-white/venice-keys-512.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/transparent/venice-keys-deep-blue-1024.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/transparent/venice-keys-deep-blue-128.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/transparent/venice-keys-deep-blue-256.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/transparent/venice-keys-deep-blue-512.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/transparent/venice-keys-deep-blue-64.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/transparent/venice-keys-off-white-1024.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/transparent/venice-keys-off-white-128.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/transparent/venice-keys-off-white-256.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/transparent/venice-keys-off-white-512.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/official-venice/keys/transparent/venice-keys-off-white-64.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons-round/venice-fyr-round-144.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons-round/venice-fyr-round-192.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons-round/venice-fyr-round-48.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons-round/venice-fyr-round-512.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons-round/venice-fyr-round-72.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons-round/venice-fyr-round-96.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons/venice-fyr-128.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons/venice-fyr-16.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons/venice-fyr-192.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons/venice-fyr-24.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons/venice-fyr-256.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons/venice-fyr-32.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons/venice-fyr-48.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons/venice-fyr-512.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons/venice-fyr-64.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/icons/venice-fyr-96.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/product/master/venice-fyr-beacon-v2-1024.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/sources/official/venice-keys-deep-blue.svg` | 4 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/sources/official/venice-keys-off-white.svg` | 4 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/sources/product/venice-fyr-beacon-monochrome.svg` | 12 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/sources/product/venice-fyr-beacon-v2.svg` | 30 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/sources/product/venice-fyr-mark-repo-baseline.svg` | 53 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/store/google-play/feature-graphic-1024x500.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/store/google-play/icon-512.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/tokens/venice-brand-colors.json` | 16 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/tokens/venice_brand_colors.xml` | 17 | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/web/favicon-16.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/web/favicon-180.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/web/favicon-192.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/web/favicon-32.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/web/favicon-48.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/Venice-Fyr-Brand-Asset-Pack/web/favicon-512.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/ayanami-rei.codex-pet/pet.json` | 8 | N |  |
| DOCUMENTATION | `docs/assets/ayanami-rei.codex-pet/spritesheet.webp` | binary | N |  |
| DOCUMENTATION | `docs/assets/store/google-play/feature-graphic-1024x500.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/store/google-play/icon-512.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-guidelines.pdf` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/DESIGN.md` | 196 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/executives/erik-voorhees.jpg` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/executives/jesse-proudman.jpg` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/keys/venice-keys-deep-blue.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/keys/venice-keys-deep-blue.svg` | 4 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/keys/venice-keys-off-white.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/keys/venice-keys-off-white.svg` | 4 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/keys/venice-keys-on-deep-blue.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/keys/venice-keys-on-deep-blue.svg` | 7 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/keys/venice-keys-on-midnight-blue.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/keys/venice-keys-on-midnight-blue.svg` | 7 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/keys/venice-keys-on-off-white.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/keys/venice-keys-on-off-white.svg` | 7 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-deep-blue.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-deep-blue.svg` | 7 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-off-white.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-off-white.svg` | 7 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-on-deep-blue.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-on-deep-blue.svg` | 10 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-on-midnight-blue.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-on-midnight-blue.svg` | 10 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-on-off-white.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-on-off-white.svg` | 10 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-deep-blue.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-deep-blue.svg` | 5 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-off-white.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-off-white.svg` | 5 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-on-deep-blue.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-on-deep-blue.svg` | 8 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-on-midnight-blue.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-on-midnight-blue.svg` | 8 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-on-off-white.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-on-off-white.svg` | 8 | N |  |
| DOCUMENTATION | `docs/assets/venice-brand-kit/venice-brand-guidelines.pdf` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-fyr-banner.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-fyr-banner.svg` | 89 | N |  |
| DOCUMENTATION | `docs/assets/venice-fyr-mark.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/venice-fyr-mark.svg` | 53 | N |  |
| DOCUMENTATION | `docs/assets/venice-fyr-social-preview.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/web/favicon-16.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/web/favicon-180.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/web/favicon-192.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/web/favicon-32.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/web/favicon-48.png` | binary | N |  |
| DOCUMENTATION | `docs/assets/web/favicon-512.png` | binary | N |  |
| DOCUMENTATION | `docs/reference/VENICE_API_SOURCE_MANIFEST.md` | 57 | N |  |
| DOCUMENTATION | `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md` | 158 | N |  |
| DOCUMENTATION | `docs/superpowers/plans/2026-08-15-android-port-milestone-1.md` | 2730 | N |  |
| DOCUMENTATION | `docs/superpowers/specs/2026-08-15-android-port-milestone-1-design.md` | 356 | N |  |
| BUILD/TOOLING | `gradle.properties` | 5 | N |  |
| BUILD/TOOLING | `gradle/libs.versions.toml` | 58 | N |  |
| BUILD/TOOLING | `gradle/wrapper/gradle-wrapper.jar` | binary | N |  |
| BUILD/TOOLING | `gradle/wrapper/gradle-wrapper.properties` | 9 | N |  |
| BUILD/TOOLING | `gradlew` | 248 | N |  |
| BUILD/TOOLING | `gradlew.bat` | 82 | N |  |
| BUILD/TOOLING | `scripts/bootstrap-desktop-source.sh` | 68 | N |  |
| BUILD/TOOLING | `scripts/bootstrap-venice-api-docs.sh` | 77 | Y |  |
| BUILD/TOOLING | `scripts/bootstrap-wrapper.sh` | 31 | N |  |
| BUILD/TOOLING | `settings.gradle.kts` | 23 | N |  |
| SDK | `venice-sdk/build.gradle.kts` | 29 | N |  |
| SDK | `venice-sdk/consumer-rules.pro` | 1 | N |  |
| SDK | `venice-sdk/src/main/AndroidManifest.xml` | 1 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/ModelType.kt` | 26 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpoints.kt` | 67 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt` | 288 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceModel.kt` | 58 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkConfig.kt` | 11 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkException.kt` | 105 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt` | 46 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioModels.kt` | 14 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt` | 121 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCapabilities.kt` | 51 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCatalog.kt` | 42 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt` | 152 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt` | 89 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulator.kt` | 51 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamChunk.kt` | 23 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParser.kt` | 14 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt` | 91 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageModels.kt` | 84 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt` | 119 | N |  |
| SDK | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoModels.kt` | 54 | N |  |
| SDK | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpointsTest.kt` | 17 | N |  |
| SDK | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | 161 | N |  |
| SDK | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepositoryTest.kt` | 102 | N |  |
| SDK | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClientTest.kt` | 220 | N |  |
| SDK | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulatorTest.kt` | 46 | N |  |
| SDK | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParserTest.kt` | 32 | N |  |
| SDK | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/VeniceParametersSerializationTest.kt` | 78 | N |  |
| SDK | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClientTest.kt` | 105 | N |  |
| SDK | `venice-sdk/src/test/resources/fixtures/chat-stream/stream-good.sse` | 9 | N |  |
| SDK | `venice-sdk/src/test/resources/fixtures/models-with-capabilities/compatibility.json` | 9 | N |  |
| SDK | `venice-sdk/src/test/resources/fixtures/models-with-capabilities/models.json` | 78 | N |  |
| SDK | `venice-sdk/src/test/resources/fixtures/models-with-capabilities/traits.json` | 10 | N |  |

## Area summary

| Area | Files |
|------|-------|
| DOCUMENTATION | 183 |
| SDK | 35 |
| APP | 34 |
| CORE:DATA | 22 |
| REPO GOVERNANCE | 15 |
| BUILD/TOOLING | 11 |
| CORE:DESIGNSYSTEM | 8 |
| CI/CD | 6 |
| CORE:COMMON | 4 |
| CORE:SECURITY | 3 |
