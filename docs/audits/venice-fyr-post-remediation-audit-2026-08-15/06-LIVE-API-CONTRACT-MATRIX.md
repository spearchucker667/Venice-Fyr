# Live API Contract Matrix

Status distinguishes known endpoint, SDK implementation, app consumption, and tests.

| Feature | Method / endpoint | Request → response MIME | SDK | App | Tests | Current status |
|---|---|---|---|---|---|---|
| Models | GET `/models` | JSON → JSON | yes | yes | yes | Implemented; runtime catalog |
| Traits | GET `/models/traits?type=` | JSON → JSON | yes | yes | text + image | Discovered modalities propagated and namespaced |
| Compatibility | GET `/models/compatibility_mapping?type=` | JSON → JSON | yes | yes | text + image | Discovered modalities propagated and namespaced |
| Chat | POST `/chat/completions` | JSON → SSE | yes | yes | yes | Streaming foundation; reasoning preserved separately |
| Chat | POST `/chat/completions` | JSON → JSON | no | no | no | Open |
| Responses | POST `/responses` | JSON → JSON/SSE | no | no | no | Open |
| Image generate | POST `/image/generate` | JSON → JSON/image | yes | yes | partial | Implemented; binary generation returns bytes |
| Image edit | POST `/image/edit` | JSON → image | yes | yes | yes | Corrected |
| Image multi-edit | POST `/image/multi-edit` | JSON → image | yes | no | yes through shared helper | Corrected SDK |
| Image upscale | POST `/image/upscale` | JSON → image/png | yes | no | yes through shared helper | Corrected SDK |
| Image background removal | POST `/image/background-remove` | JSON/multipart → image/png | no | no | no | Open |
| Image styles | GET `/image/styles` | JSON | no | no | no | Open |
| Compatibility images | POST `/images/generations` | JSON → JSON | no | no | no | Open |
| Video queue | POST `/video/queue` | JSON → JSON | yes | no | serialization/build | Implemented SDK |
| Video quote | POST `/video/quote` | JSON → JSON | yes | no | build | Implemented SDK |
| Video retrieve | POST `/video/retrieve` | JSON → JSON/video | yes | no | yes | Correct state discrimination |
| Video complete | POST `/video/complete` | JSON → JSON | yes | no | build | Returns `success` |
| Video transcription | POST `/video/transcriptions` | JSON → JSON/text | yes | no | build | Implemented SDK |
| Speech | POST `/audio/speech` | JSON → audio | yes | no | build | Spec fields corrected |
| Audio quote/queue/retrieve/complete | POST | JSON → JSON/audio | yes | no | yes | Implemented SDK; no durable poller |
| Audio transcription | POST `/audio/transcriptions` | multipart → JSON/text | yes | no | build | Implemented SDK |
| Voice cloning | POST `/audio/voices` | multipart → JSON | yes | no | build | Implemented SDK |
| Embeddings | POST `/embeddings` | JSON → JSON | no | no | no | Open |
| Characters | GET `/characters*` | JSON | no | no | no | Open |
| Augment | POST `/augment/*` | JSON/multipart → JSON/text | no | no | no | Open |
| Billing/API keys/x402 | various | JSON | no | no | no | Intentionally deferred |

All implemented JSON calls use Bearer auth and structured non-2xx parsing. Binary media helpers validate a non-empty body and expected top-level MIME family. Media request cancellation uses the shared cancellation-native OkHttp bridge.
