# Electron → Android Responsibility Map

Do not port Electron IPC mechanically. Port the *trust boundary* represented by each Electron service.

| Electron responsibility | Android replacement |
|---|---|
| `electron/services/secureStore.ts` | `core/security` Android Keystore AES-GCM + credential-protected storage |
| `electron/services/veniceClient.ts` + `src/services/veniceClient/*` | `venice-sdk` OkHttp/coroutines, typed endpoint clients, SSE parser, multipart encoders |
| `electron/services/generatedMediaStore.ts` | app-private content-addressed media repository + Room metadata |
| `electron/services/generatedMediaRecoveryQueue.ts` | bounded app cache + persisted recovery metadata + WorkManager retry |
| `electron/services/generatedVideoDownload.ts` / retrieve services | durable WorkManager/User-Initiated Data Transfer job + Media3/SAF export |
| `electron/services/backgroundTaskManager.ts` | WorkManager for deferrable work; user-initiated transfer/foreground notification where Android requires it |
| `electron/services/chatStorage.ts` / folder stores | Room 3 database + transactions + recovery journal |
| `electron/services/conversationVault.ts` | encrypted profile-local data/vault abstraction; biometric unlock optional, not mandatory for API correctness |
| `electron/services/backupCrypto.ts` / sync services | `.vfbackup` compatible crypto, SAF tree URI sync target, WorkManager reconciliation |
| `electron/agent/workspace/*` | SAF persisted directory grants; canonical URI/document-ID policy; never raw arbitrary filesystem traversal |
| `electron/agent/documents/*` | native Kotlin managed-doc engine with immutable revisions and explicit diff approval |
| `electron/utils/customProtocolAccess.ts` | `content://` URI provider/repository abstractions; no custom desktop scheme required |
| Native Electron dialogs | Activity Result Contracts: Photo Picker, OpenDocument, CreateDocument, OpenDocumentTree |
| `safeStorage` | Android Keystore; ciphertext in app-private credential-protected storage |
| renderer localStorage / IndexedDB | DataStore for preferences; Room for relational/history/media/agent state |
| HTML media playback | Media3 ExoPlayer / Compose UI |
| browser-side responsive CSS | Compose adaptive layouts and window-size classes |

## Source directories to consult before each feature

- `src/config/tabs.ts` — canonical stable feature IDs.
- `src/types/venice.ts` — current model/capability/chat contracts.
- `src/services/modelService.ts` — runtime model discovery behavior.
- `electron/services/guardPipeline.ts` and `src/services/veniceClient/fetch.ts` — local/provider safety semantics.
- `electron/services/generatedMedia*` and `src/stores/media-store*` — media custody/integrity/recovery semantics.
- `electron/services/chatFolder*`, `chatStorage*`, `conversationVault*` — history/profile isolation and durability.
- `electron/agent/**` — tool registry, approvals, document/workspace constraints.
- `docs/reference/Venice_swagger_api.yaml` — tracked wire contract.
- `docs/reference/VENICE_API_SOURCE_MANIFEST.md` — upstream precedence rules.
