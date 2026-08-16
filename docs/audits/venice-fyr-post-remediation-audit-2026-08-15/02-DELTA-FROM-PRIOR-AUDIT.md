# Delta From Prior Audit

The historical audit captured `1da3142`; this audit began at `df8f383`. Intermediate commits added the audit evidence, CI, compile fixes, framework-owned ViewModels, profile/message transaction fixes, persisted chat model selection, structured stream errors, multiline SSE parsing, strict truncation failure, and cancellation-native chat transport.

## Prior P0/P1 reconciliation

| Prior finding | Current classification | Evidence |
|---|---|---|
| `BASELINE-01` compile failure | FIXED | SDK/app compilation and build gates execute. |
| `BUILD-01` no CI | FIXED | `.github/workflows/android-ci.yml` runs wrapper validation, tests, lint, debug app, release AAR, and unsigned release app. |
| `APP-UI-001` / `ARCH-01` raw remembered ViewModels | FIXED | `VeniceForgeApp` uses `viewModel(..., factory=...)`. |
| `APP-UI-002` dead fallback | FIXED | `FeatureCatalog.byId` uses `firstOrNull`. |
| `ARCH-02` duplicate latest user message | FALSE_OLD_FINDING | Revalidation proved repository history construction did not duplicate the current message. |
| `DATA-03/04/05` profile/message atomicity | FIXED | transactions, insert-ignore default profile, and scoped updates are present with Room tests. |
| `DATA-09` docs claimed encrypted Room | SUPERSEDED | security docs now explicitly state unencrypted app-private Room data. |
| `SEC-01/02`, `CHAT-07/08/10/11` stream privacy/framing/cancellation | FIXED | structured/redacted errors, SSE event framing, strict EOF handling, and async cancellation tests. |
| `IMG-01` binary image parsed as JSON | FIXED_THIS_AUDIT | binary response wrapper, MIME/body validation, app consumer update, tests. |
| `VID-03` JSON completed reported processing | FIXED_THIS_AUDIT | `CompletedRemote`, `CompletedBinary`, and `UnknownStatus`. |
| `AUD-01/02` queued audio/transcription missing | FIXED_THIS_AUDIT | typed queue lifecycle plus multipart transcription/voice API. |
| Chat reasoning omission | FIXED_THIS_AUDIT (SDK); PARTIAL app | typed request controls and separate deltas; persisted reasoning UI remains open. |
| `CHAT-01/02/03/04` broader chat completeness | STILL_PRESENT | current public model remains streaming-first and string-content/function-tool subset. |
| `SDK-CORE-01` modality discovery | FIXED_THIS_AUDIT | discovered model modalities are forwarded to traits/compatibility and non-text keys are namespaced. |
| `SDK-CORE-02` modality defaults | FIXED | `defaultModelIdFor(type)` resolves namespaced runtime traits, rejects orphan/offline targets, and falls back only to an online model of the requested modality. |
| `VID-02/04` quote/transcription missing | FIXED_THIS_AUDIT | typed methods added. |
| `BUILD-03` signing absent | SUPERSEDED FOR ALPHA / release gate | CI intentionally validates unsigned release; public signing remains external. |

No prior P0 is currently open. Full current findings are in `07-P0-FINDINGS.md` through `10-P3-FINDINGS.md`.
