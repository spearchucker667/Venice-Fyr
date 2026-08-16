# Feature Parity Matrix

Status meanings: **Foundation** = a real Android primitive is present; **Scaffolded** = stable route/contract exists but desktop behavior is not yet ported; **Parity** = desktop behavior and acceptance tests have been implemented.

| Stable ID | Desktop area | Android now | Android target |
|---|---|---|---|
| `chat` | Chat | Foundation | Cancellation-native SSE with strict terminal-state handling, structured HTTP failures, multiline parsing, separate reasoning deltas, and profile-scoped answer persistence. Non-streaming completions, multimodal content, full tools, reasoning persistence/UI, projects, and agent approvals remain incomplete. |
| `character-chats` | Character Chats | Scaffolded | Hosted/local cards, isolated identity, greetings, personas, lorebooks, scene generation |
| `history` | History | Scaffolded | Room persistence, folders, lock/import/export/recovery |
| `image` | Image Studio | Foundation | Generate/edit with runtime capability gating, corrected binary edit responses, Photo Picker input, and ImageViewModel/ImageScreen. Durable media storage/export, multipart, multi-edit/background removal/styles UI remain incomplete. |
| `media` | Media Studio | Scaffolded | Content-addressed media store, gallery, compare, lineage, Save As, manifest export |
| `image-inspector` | Image Inspector | Scaffolded | Photo Picker/SAF import, bounded decode, vision analysis, prompt reconstruction |
| `prompts` | Prompts | Scaffolded | Scoped prompt library, tags, immutable versions |
| `scenes` | Scene Composer | Scaffolded | Touch canvas, prompts/media/models, persisted graph |
| `audio` | Audio Studio | Foundation (SDK) | SDK speech, multipart transcription, and voice-cloning contracts exist; Media3 UI playback/persistence remain planned. |
| `music` | Music Studio | Foundation (SDK) | Typed quote/queue/retrieve/complete contracts exist; durable WorkManager polling and UI remain absent. |
| `video` | Video Studio | Foundation (SDK) | SDK queue/quote/retrieve/complete/transcription contracts exist with truthful binary/JSON-complete discrimination; durable jobs and UI remain planned. |
| `embeddings` | Embeddings | Scaffolded | Vector requests/inspection |
| `search` | Research | Scaffolded | Venice/Jina search + scrape + synthesis + citations + saved sessions |
| `characters` | Characters | Scaffolded | Hosted/local libraries, image cache, card import/export |
| `character-creator` | Character Creator | Scaffolded | AI authoring pipeline and process events |
| `rp-studio` | RP Studio | Scaffolded | ST Card Studio, personas, scenarios, lorebooks, multi-character chat |
| `workflows` | Workflows | Scaffolded | Template/versioned graph executor, confirmations, paid-task guards |
| `documents` | Documents | Scaffolded | Managed docs, immutable revisions, SAF directory grants/search/edit/export |
| `playground` | Playground | Scaffolded | Multi-model visual workflow execution |
| `privacy` | Privacy | Foundation | Storage inventory, encrypted `.vfbackup`, sync folder, purge/maintenance |
| `settings` | Config | Foundation | Keystore credential save/remove + live models; add providers/i18n/themes/safety/sync |
| `status` | Status | Foundation | Add tasks, connectivity, rate limits/billing, redacted logs/diagnostics |

## Non-feature contracts that also require parity

- Profiles and strict profile data isolation.
- Local Family Safe Mode remains separate from Venice provider `safe_mode`.
- Provider `safe_mode=false` must remain explicit where the desktop client explicitly sends it; Android must not silently drop the field.
- Dynamic runtime model capability discovery via `/models`, `/models/traits`, `/models/compatibility_mapping`.
- No hardcoded image-edit model allowlist; use capabilities/model metadata.
- Media actions are capability-gated by the action/request target, not incorrectly inherited from a source asset's generation model.
- Main-owned Electron IPC boundaries must become Android process/storage/service boundaries, not disappear.
- Secret, prompt, response-body, and local-path redaction in logs/diagnostics.
- No telemetry by default.
- Preserve the desktop fallback-provider capability registry and its implemented/deferred distinction; see `PROVIDER_PARITY.md`.
- Background media operations survive activity recreation/process death.
- Native export/import uses SAF/Photo Picker/content URIs; never require broad filesystem permission.
