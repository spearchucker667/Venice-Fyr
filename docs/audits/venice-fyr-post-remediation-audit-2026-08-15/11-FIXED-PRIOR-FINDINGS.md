# Fixed Prior Findings

Confirmed fixed before this audit: original SDK compile import, ImageScreen compile blocker, missing CI, raw remembered ViewModels, dead feature fallback, profile first-launch race, non-transactional assistant updates, duplicate submission guards, cancellation exception handling, API-key IO dispatcher use, chat model persistence, raw stream error leakage, non-structured HTTP stream errors, multiline SSE framing, ignored SSE control fields, inferred success on truncation, and blocked-read cancellation.

Fixed during this audit:

- `IMG-01` / `LIVE-P1-01`: edit, multi-edit, and upscale return binary media with MIME/request metadata.
- `VID-03` / `LIVE-P1-02`: JSON `COMPLETED` is not represented as processing; queue-time `download_url` remains caller-owned.
- `AUD-01`, `AUD-02` / `LIVE-P1-03`: typed audio quote/queue/retrieve/complete, transcription, and voice-cloning surface.
- `LIVE-P1-04`: typed reasoning request controls, separate streamed reasoning chunks, accumulator preservation, encrypted placeholder tests.
- `VID-02`, `VID-04`: video quote and transcription surface.
- `AUD-03`: speech fields aligned and unsupported audio `safe_mode` removed.
- `CHAT-05`, `CHAT-06`: misleading unimplemented E2EE and image-only chat safety switches removed.
- `IMG-09`: HTTP 402 classified as `PaymentRequired`.
- `SDK-CORE-01`: discovered model modalities are propagated to traits/compatibility queries; cancellation is no longer swallowed.
- `SDK-CORE-02`: modality defaults resolve through `defaultModelIdFor(type)` and reject orphan or offline trait targets.
- Compiler/test hygiene: redundant OkHttp body safe calls, repeated coroutine-test opt-in warnings, and the stale endpoint schema comment were corrected.
