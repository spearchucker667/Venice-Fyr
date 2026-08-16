# File Audit Ledger

Coverage method: every current tracked or task-created file was classified; all production source, build/configuration, tests, manifests/resources, scripts, and current documentation were reviewed directly or through focused call-site/static-search traces. The original audit directory is immutable historical evidence and was reconciled through its executive, matrix, finding, revalidation, and validation records. Binary assets were checked by path, type, duplicate hash evidence, references, and licensing status.

| File | Lines | Reviewed | Changed since prior audited production SHA `1da3142` | Findings / notes |
|---|---:|---|---|---|
| `.gitattributes` | 2 | Yes | No | Repository inventory/hygiene review |
| `.github/CODEOWNERS` | 4 | Yes | No | Repository inventory/hygiene review |
| `.github/ISSUE_TEMPLATE/bug_report.yml` | 85 | Yes | No | Direct source/config/test/static review |
| `.github/ISSUE_TEMPLATE/config.yml` | 8 | Yes | No | Direct source/config/test/static review |
| `.github/ISSUE_TEMPLATE/documentation.yml` | 27 | Yes | No | Direct source/config/test/static review |
| `.github/ISSUE_TEMPLATE/feature_request.yml` | 50 | Yes | No | Direct source/config/test/static review |
| `.github/PULL_REQUEST_TEMPLATE.md` | 57 | Yes | No | Documentation/contract review |
| `.github/workflows/android-ci.yml` | 66 | Yes | Yes | Direct source/config/test/static review |
| `.gitignore` | 20 | Yes | Yes | Repository inventory/hygiene review |
| `AGENTS.md` | 142 | Yes | Yes | Documentation/contract review |
| `ANDROID_PORT_HANDOFF.md` | 233 | Yes | No | Documentation/contract review |
| `app/build.gradle.kts` | 68 | Yes | No | Direct source/config/test/static review |
| `app/src/main/AndroidManifest.xml` | 23 | Yes | No | Direct source/config/test/static review |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatScreen.kt` | 142 | Yes | Yes | Direct source/config/test/static review |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt` | 316 | Yes | Yes | Direct source/config/test/static review |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalog.kt` | 48 | Yes | Yes | Direct source/config/test/static review |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageScreen.kt` | 227 | Yes | Yes | Direct source/config/test/static review |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt` | 141 | Yes | Yes | Direct source/config/test/static review |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/MainActivity.kt` | 17 | Yes | No | Direct source/config/test/static review |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/ui/ConfigScreen.kt` | 173 | Yes | No | Direct source/config/test/static review |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt` | 279 | Yes | Yes | Direct source/config/test/static review |
| `app/src/main/res/drawable/ic_launcher_background.xml` | 7 | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | 15 | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | 12 | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/drawable/ic_venice_keys_deep_blue.xml` | 9 | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/drawable/ic_venice_keys_off_white.xml` | 9 | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | 6 | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | 6 | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/mipmap-hdpi/ic_launcher_round.png` | binary | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/mipmap-hdpi/ic_launcher.png` | binary | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/mipmap-mdpi/ic_launcher_round.png` | binary | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/mipmap-mdpi/ic_launcher.png` | binary | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/mipmap-xhdpi/ic_launcher_round.png` | binary | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/mipmap-xhdpi/ic_launcher.png` | binary | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png` | binary | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` | binary | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png` | binary | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` | binary | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/values-v27/themes.xml` | 5 | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/values/strings.xml` | 19 | Yes | Yes | Asset/resource reference and policy review |
| `app/src/main/res/values/themes.xml` | 8 | Yes | No | Asset/resource reference and policy review |
| `app/src/main/res/xml/network_security_config.xml` | 3 | Yes | No | Asset/resource reference and policy review |
| `app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt` | 434 | Yes | Yes | Direct source/config/test/static review |
| `app/src/test/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalogTest.kt` | 17 | Yes | Yes | Direct source/config/test/static review |
| `app/src/test/resources/robolectric.properties` | 1 | Yes | No | Direct source/config/test/static review |
| `build.gradle.kts` | 5 | Yes | No | Direct source/config/test/static review |
| `CHANGELOG.md` | 52 | Yes | Yes | Documentation/contract review |
| `CODE_OF_CONDUCT.md` | 33 | Yes | No | Documentation/contract review |
| `CONTRIBUTING.md` | 168 | Yes | No | Documentation/contract review |
| `core/common/build.gradle.kts` | 17 | Yes | No | Direct source/config/test/static review |
| `core/common/src/main/AndroidManifest.xml` | 1 | Yes | No | Direct source/config/test/static review |
| `core/common/src/main/java/io/github/spearchucker667/veniceforge/core/common/Redactor.kt` | 17 | Yes | No | Direct source/config/test/static review |
| `core/common/src/test/java/io/github/spearchucker667/veniceforge/core/common/RedactorTest.kt` | 15 | Yes | No | Direct source/config/test/static review |
| `core/data/build.gradle.kts` | 56 | Yes | No | Direct source/config/test/static review |
| `core/data/schemas/io.github.spearchucker667.veniceforge.core.data.AppDatabase/1.json` | 477 | Yes | No | Direct source/config/test/static review |
| `core/data/src/main/AndroidManifest.xml` | 2 | Yes | No | Direct source/config/test/static review |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt` | 47 | Yes | No | Direct source/config/test/static review |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/Converters.kt` | 21 | Yes | No | Direct source/config/test/static review |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ConversationDao.kt` | 33 | Yes | No | Direct source/config/test/static review |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageDao.kt` | 31 | Yes | Yes | Direct source/config/test/static review |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageToolCallDao.kt` | 17 | Yes | No | Direct source/config/test/static review |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ProfileDao.kt` | 33 | Yes | Yes | Direct source/config/test/static review |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/DataServices.kt` | 22 | Yes | No | Direct source/config/test/static review |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ConversationEntity.kt` | 43 | Yes | No | Direct source/config/test/static review |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ConversationFolderEntity.kt` | 27 | Yes | No | Direct source/config/test/static review |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageEntity.kt` | 38 | Yes | No | Direct source/config/test/static review |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageToolCallEntity.kt` | 35 | Yes | No | Direct source/config/test/static review |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ProfileEntity.kt` | 14 | Yes | No | Direct source/config/test/static review |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt` | 111 | Yes | Yes | Direct source/config/test/static review |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ProfileRepository.kt` | 27 | Yes | Yes | Direct source/config/test/static review |
| `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ChatRepositoryTest.kt` | 99 | Yes | Yes | Direct source/config/test/static review |
| `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/MigrationTest.kt` | 51 | Yes | No | Direct source/config/test/static review |
| `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ProfileIsolationTest.kt` | 67 | Yes | No | Direct source/config/test/static review |
| `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ProfileRepositoryTest.kt` | 52 | Yes | Yes | Direct source/config/test/static review |
| `core/data/src/test/resources/robolectric.properties` | 1 | Yes | No | Direct source/config/test/static review |
| `core/designsystem/build.gradle.kts` | 24 | Yes | No | Direct source/config/test/static review |
| `core/designsystem/src/main/AndroidManifest.xml` | 1 | Yes | No | Direct source/config/test/static review |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/CodexPet.kt` | 128 | Yes | No | Direct source/config/test/static review |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceColors.kt` | 93 | Yes | No | Direct source/config/test/static review |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceForgeTheme.kt` | 26 | Yes | No | Direct source/config/test/static review |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceLoadingIndicator.kt` | 86 | Yes | No | Direct source/config/test/static review |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceTypography.kt` | 129 | Yes | No | Direct source/config/test/static review |
| `core/designsystem/src/main/res/drawable-nodpi/ayanami_rei_spritesheet.webp` | binary | Yes | No | Asset/resource reference and policy review |
| `core/security/build.gradle.kts` | 18 | Yes | No | Direct source/config/test/static review |
| `core/security/src/main/AndroidManifest.xml` | 1 | Yes | No | Direct source/config/test/static review |
| `core/security/src/main/java/io/github/spearchucker667/veniceforge/core/security/SecureSecretStore.kt` | 100 | Yes | No | Direct source/config/test/static review |
| `docs/API_INTEGRATION_GUIDE.md` | 100 | Yes | Yes | Documentation/contract review |
| `docs/assets/ayanami-rei.codex-pet/pet.json` | 8 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/ayanami-rei.codex-pet/spritesheet.webp` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/product-source/venice-fyr-beacon-monochrome.svg` | 11 | Yes | Yes | Asset/resource reference and policy review |
| `docs/assets/product-source/venice-fyr-beacon-v2.svg` | 29 | Yes | Yes | Asset/resource reference and policy review |
| `docs/assets/store/google-play/feature-graphic-1024x500.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/store/google-play/icon-512.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/DESIGN.md` | 196 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/executives/erik-voorhees.jpg` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/executives/jesse-proudman.jpg` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/keys/venice-keys-deep-blue.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/keys/venice-keys-deep-blue.svg` | 4 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/keys/venice-keys-off-white.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/keys/venice-keys-off-white.svg` | 4 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/keys/venice-keys-on-deep-blue.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/keys/venice-keys-on-deep-blue.svg` | 7 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/keys/venice-keys-on-midnight-blue.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/keys/venice-keys-on-midnight-blue.svg` | 7 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/keys/venice-keys-on-off-white.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/keys/venice-keys-on-off-white.svg` | 7 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-deep-blue.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-deep-blue.svg` | 7 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-off-white.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-off-white.svg` | 7 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-on-deep-blue.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-on-deep-blue.svg` | 10 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-on-midnight-blue.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-on-midnight-blue.svg` | 10 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-on-off-white.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/lockup/venice-logo-lockup-on-off-white.svg` | 10 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-deep-blue.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-deep-blue.svg` | 5 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-off-white.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-off-white.svg` | 5 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-on-deep-blue.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-on-deep-blue.svg` | 8 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-on-midnight-blue.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-on-midnight-blue.svg` | 8 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-on-off-white.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/logos/wordmark/venice-wordmark-on-off-white.svg` | 8 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-brand-kit/venice-brand-guidelines.pdf` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-fyr-banner.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-fyr-banner.svg` | 88 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-fyr-mark.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-fyr-mark.svg` | 52 | Yes | No | Asset/resource reference and policy review |
| `docs/assets/venice-fyr-social-preview.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/web/favicon-16.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/web/favicon-180.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/web/favicon-192.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/web/favicon-32.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/web/favicon-48.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/assets/web/favicon-512.png` | binary | Yes | No | Asset/resource reference and policy review |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/00-EXECUTIVE-SUMMARY.md` | 86 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/01-REPOSITORY-SNAPSHOT.md` | 60 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/02-FILE-AUDIT-LEDGER.md` | 349 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/03-ARCHITECTURE-MAP.md` | 274 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/04-ANDROID-ARCHITECTURE-AUDIT.md` | 186 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/05-VENICE-SOURCE-OF-TRUTH.md` | 258 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/06-VENICE-API-CONTRACT-MATRIX.md` | 576 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/07-SDK-PUBLIC-API-AUDIT.md` | 209 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/08-P0-FINDINGS.md` | 131 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/09-P1-FINDINGS.md` | 2092 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/10-P2-FINDINGS.md` | 4390 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/11-P3-FINDINGS.md` | 1322 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/12-TEST-COVERAGE-GAPS.md` | 171 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/13-SECURITY-PRIVACY-AUDIT.md` | 108 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/14-ANDROID-LIFECYCLE-STORAGE-AUDIT.md` | 201 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/15-PERFORMANCE-CONCURRENCY-AUDIT.md` | 279 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/16-CI-RELEASE-AUDIT.md` | 156 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/17-DOCUMENTATION-DRIFT.md` | 98 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/18-REMEDIATION-PLAN.md` | 305 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/19-VALIDATION-RESULTS.md` | 147 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/20-UNRESOLVED-QUESTIONS.md` | 189 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/22-ASSET-CONSOLIDATION.md` | 81 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/app-viewmodels.md` | 576 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/FINDINGS.json` | 2103 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/findings/app-ui.md` | 717 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/findings/architecture.md` | 534 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/findings/baseline.md` | 97 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/findings/build.md` | 318 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/findings/chat.md` | 733 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/findings/core-data.md` | 655 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/findings/docs.md` | 343 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/findings/hygiene.md` | 113 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/findings/image.md` | 424 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/findings/sdk-core.md` | 801 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/findings/security.md` | 451 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/findings/tests.md` | 553 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/findings/video-audio.md` | 590 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/matrix/chat.md` | 159 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/matrix/image.md` | 90 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/matrix/models.md` | 132 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/matrix/video-audio.md` | 175 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/revalidation/app.md` | 526 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/revalidation/assets.md` | 363 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/revalidation/build.md` | 278 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/revalidation/chat-sdkcore.md` | 268 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/revalidation/data-security-docs-tests.md` | 445 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/revalidation/gates-wp-r1.md` | 180 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/revalidation/media.md` | 519 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/revalidation/spec-allowlist.md` | 193 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/run-validation.sh` | 52 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/validation-raw.log` | 1703 | Historical evidence | Yes | Prior audit; reconciled, not rewritten |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/00-EXECUTIVE-SUMMARY.md` | 9 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/01-REPOSITORY-SNAPSHOT.md` | 16 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/02-DELTA-FROM-PRIOR-AUDIT.md` | 27 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/03-FILE-AUDIT-LEDGER.md` | 297 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/04-ARCHITECTURE-STATE.md` | 7 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/05-VENICE-SOURCE-BASELINE.md` | 11 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/06-LIVE-API-CONTRACT-MATRIX.md` | 34 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/07-P0-FINDINGS.md` | 3 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/08-P1-FINDINGS.md` | 70 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/09-P2-FINDINGS.md` | 185 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/10-P3-FINDINGS.md` | 3 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/11-FIXED-PRIOR-FINDINGS.md` | 17 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/12-REGRESSIONS.md` | 3 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/13-TEST-COVERAGE-GAPS.md` | 7 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/14-SECURITY-PRIVACY.md` | 5 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/15-LIFECYCLE-PERSISTENCE.md` | 5 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/16-STREAMING-TOOLS-REASONING.md` | 7 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/17-MEDIA-JOBS.md` | 9 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/18-CI-RELEASE.md` | 5 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/19-ASSETS-LICENSING.md` | 5 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/20-DOCUMENTATION-DRIFT.md` | 5 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/21-REMEDIATION-PLAN.md` | 29 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/22-VALIDATION-RESULTS.md` | 40 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/23-UNRESOLVED-QUESTIONS.md` | 10 | Yes | Yes | Documentation/contract review |
| `docs/audits/venice-fyr-post-remediation-audit-2026-08-15/FINDINGS.json` | 23 | Yes | Yes | Direct source/config/test/static review |
| `docs/BRANDING.md` | 145 | Yes | Yes | Documentation/contract review |
| `docs/DESKTOP_SOURCE_BOOTSTRAP.md` | 286 | Yes | No | Documentation/contract review |
| `docs/DEVELOPMENT_GUIDE.md` | 140 | Yes | No | Documentation/contract review |
| `docs/ELECTRON_TO_ANDROID_MAP.md` | 35 | Yes | No | Documentation/contract review |
| `docs/FEATURE_PARITY_MATRIX.md` | 43 | Yes | Yes | Documentation/contract review |
| `docs/GETTING_STARTED.md` | 130 | Yes | No | Documentation/contract review |
| `docs/GITHUB_DOCS_INDEX.md` | 54 | Yes | No | Documentation/contract review |
| `docs/PROVIDER_PARITY.md` | 34 | Yes | No | Documentation/contract review |
| `docs/reference/VENICE_API_SOURCE_MANIFEST.md` | 57 | Yes | Yes | Documentation/contract review |
| `docs/RELEASE_CHECKLIST.md` | 58 | Yes | No | Documentation/contract review |
| `docs/reviews/archive/AGENT_HANDOFF_REVIEW_FIRST.md` | 367 | Yes | Yes | Documentation/contract review |
| `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md` | 158 | Yes | No | Documentation/contract review |
| `docs/SDK_EXAMPLES.md` | 184 | Yes | Yes | Documentation/contract review |
| `docs/SDK_GUIDE.md` | 79 | Yes | No | Documentation/contract review |
| `docs/SECURITY_AND_STORAGE_CONTRACT.md` | 15 | Yes | Yes | Documentation/contract review |
| `docs/superpowers/plans/2026-08-15-android-port-milestone-1.md` | 2730 | Yes | No | Documentation/contract review |
| `docs/superpowers/specs/2026-08-15-android-port-milestone-1-design.md` | 356 | Yes | No | Documentation/contract review |
| `docs/TROUBLESHOOTING.md` | 148 | Yes | No | Documentation/contract review |
| `docs/USER_GUIDE.md` | 59 | Yes | No | Documentation/contract review |
| `docs/VENICE_API_PORT_MATRIX.md` | 59 | Yes | No | Documentation/contract review |
| `docs/VENICE_API_SOURCE_BOOTSTRAP.md` | 88 | Yes | No | Documentation/contract review |
| `gradle.properties` | 5 | Yes | No | Direct source/config/test/static review |
| `gradle/libs.versions.toml` | 58 | Yes | No | Direct source/config/test/static review |
| `gradle/wrapper/gradle-wrapper.jar` | binary | Yes | No | Repository inventory/hygiene review |
| `gradle/wrapper/gradle-wrapper.properties` | 9 | Yes | No | Direct source/config/test/static review |
| `gradlew` | 248 | Yes | No | Repository inventory/hygiene review |
| `gradlew.bat` | 82 | Yes | No | Repository inventory/hygiene review |
| `LEGAL.md` | 88 | Yes | No | Documentation/contract review |
| `LICENSE` | 201 | Yes | No | Documentation/contract review |
| `NOTICE` | 22 | Yes | No | Documentation/contract review |
| `PRIVACY.md` | 100 | Yes | No | Documentation/contract review |
| `README.md` | 292 | Yes | Yes | Documentation/contract review |
| `scripts/bootstrap-desktop-source.sh` | 68 | Yes | No | Direct source/config/test/static review |
| `scripts/bootstrap-venice-api-docs.sh` | 77 | Yes | No | Direct source/config/test/static review |
| `scripts/bootstrap-wrapper.sh` | 31 | Yes | No | Direct source/config/test/static review |
| `SECURITY.md` | 81 | Yes | No | Documentation/contract review |
| `settings.gradle.kts` | 23 | Yes | No | Direct source/config/test/static review |
| `SOURCE_BASELINE.md` | 74 | Yes | Yes | Documentation/contract review |
| `SUPPORT.md` | 59 | Yes | No | Documentation/contract review |
| `venice-sdk/build.gradle.kts` | 29 | Yes | No | Direct source/config/test/static review |
| `venice-sdk/consumer-rules.pro` | 1 | Yes | No | Direct source/config/test/static review |
| `venice-sdk/src/main/AndroidManifest.xml` | 1 | Yes | No | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt` | 193 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioModels.kt` | 135 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/BinaryMediaResult.kt` | 24 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt` | 143 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCapabilities.kt` | 51 | Yes | No | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCatalog.kt` | 62 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt` | 211 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt` | 112 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulator.kt` | 55 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamChunk.kt` | 24 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParser.kt` | 28 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt` | 90 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageModels.kt` | 87 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/ModelType.kt` | 25 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpoints.kt` | 67 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt` | 331 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceModel.kt` | 58 | Yes | No | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkConfig.kt` | 11 | Yes | No | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkException.kt` | 114 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt` | 130 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoModels.kt` | 113 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClientTest.kt` | 88 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepositoryTest.kt` | 128 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClientTest.kt` | 341 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulatorTest.kt` | 57 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParserTest.kt` | 48 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/VeniceParametersSerializationTest.kt` | 98 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClientTest.kt` | 210 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpointsTest.kt` | 17 | Yes | No | Direct source/config/test/static review |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | 187 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClientTest.kt` | 100 | Yes | Yes | Direct source/config/test/static review |
| `venice-sdk/src/test/resources/fixtures/chat-stream/stream-good.sse` | 9 | Yes | No | Repository inventory/hygiene review |
| `venice-sdk/src/test/resources/fixtures/models-with-capabilities/compatibility.json` | 9 | Yes | No | Direct source/config/test/static review |
| `venice-sdk/src/test/resources/fixtures/models-with-capabilities/models.json` | 78 | Yes | No | Direct source/config/test/static review |
| `venice-sdk/src/test/resources/fixtures/models-with-capabilities/traits.json` | 10 | Yes | No | Direct source/config/test/static review |
