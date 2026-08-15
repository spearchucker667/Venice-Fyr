# matrix/models.md

Coverage matrix for Venice `/models`, `/models/traits`, and `/models/compatibility_mapping` response fields vs. the `:venice-sdk` model/capability classes.

**Legend:**
- ✅ Covered — field is parsed and exposed in a public SDK property.
- ⚠️ Partial — field is parsed but not fully exposed, or exposed with caveats.
- ❌ Not covered — field is ignored by the SDK parser.
- N/A — field is not part of the swagger schema for this endpoint.

---

## 1. GET `/models` response (`ModelResponse`)

| swagger field | SDK property | Status | Notes |
|---------------|--------------|--------|-------|
| `id` | `VeniceModel.id` | ✅ | Non-null string. |
| `object` | `VeniceModel.objectType` | ✅ | Defaults to `"model"`. |
| `created` | `VeniceModel.created` | ✅ | `Long?`. Swagger type is `number`. |
| `owned_by` | `VeniceModel.ownedBy` | ✅ | Defaults to `"venice.ai"`. |
| `type` | `VeniceModel.type` | ✅ | Defaults to `"text"` if absent. |
| `model_spec` | `VeniceModel.modelSpec` | ✅ | Nested `ModelSpec`. |
| `context_length` | — | ❌ | Top-level OpenAI-compatible context length; not exposed. |
| `discount_to_user` | — | ❌ | Reseller discount; not exposed. |

## 2. `model_spec` fields

| swagger field | SDK property | Status | Notes |
|---------------|--------------|--------|-------|
| `name` | `ModelSpec.name` / `ModelCapabilities.name` | ✅ | |
| `description` | `ModelSpec.description` / `ModelCapabilities.description` | ✅ | |
| `modelSource` | `ModelSpec.modelSource` | ✅ | |
| `availableContextTokens` | `ModelSpec.availableContextTokens`, `ModelCapabilities.availableContextTokens`, `ModelCapabilities.maxContextTokens` | ✅ | `maxContextTokens` is `Int?`; see SDK-CORE-11. |
| `maxCompletionTokens` | `ModelSpec.maxCompletionTokens`, `ModelCapabilities.maxCompletionTokens` | ✅ | `Int?`; see SDK-CORE-11. |
| `privacy` | `ModelSpec.privacy`, `ModelCapabilities.privacy` | ✅ | Enum `private`/`anonymized` in swagger; stored as `String?`. |
| `uncensored` | `ModelSpec.uncensored`, `ModelCapabilities.uncensored` | ✅ | |
| `offline` | `ModelSpec.offline`, `ModelCapabilities.offline` | ✅ | Used to derive `supportsStreaming`; see SDK-CORE-04. |
| `beta` | `ModelSpec.beta` | ✅ | |
| `betaModel` | `ModelSpec.betaModel` | ✅ | |
| `traits` | `ModelSpec.traits`, `ModelCapabilities.traits` | ✅ | |
| `regionRestrictions` | — | ❌ | Not parsed. |
| `deprecation` | — | ❌ | `autoRemap`, `date`, `removesAt`, `replacementModelId`, `startsAt` not parsed. |
| `pricing` | — | ❌ | Token/image/audio/video pricing objects not parsed. |
| `embeddingDimensions` | — | ❌ | Embedding-specific. |
| `maxInputTokens` | — | ❌ | Embedding-specific. |
| `supportsCustomDimensions` | — | ❌ | Embedding-specific. |
| `supports_lyrics` | — | ❌ | Music-specific. |
| `lyrics_required` | — | ❌ | Music-specific. |
| `supports_force_instrumental` | — | ❌ | Music-specific. |
| `supports_loop` | — | ❌ | Music-specific. |
| `voices` | — | ❌ | TTS/music-specific. |
| `voice_cloning` | — | ❌ | TTS-specific object. |
| `default_voice` | — | ❌ | Music-specific. |
| `supports_custom_voice_id` | — | ❌ | TTS/music-specific. |
| `supports_language_code` | — | ❌ | Music-specific. |
| `supports_speed` | — | ❌ | Music-specific. |
| `default_speed` | — | ❌ | Music-specific. |
| `min_speed` | — | ❌ | Music-specific. |
| `max_speed` | — | ❌ | Music-specific. |
| `duration_options` | — | ❌ | Music-specific. |
| `min_duration` | — | ❌ | Music-specific. |
| `max_duration` | — | ❌ | Music-specific. |
| `default_duration` | — | ❌ | Music-specific. |
| `supported_formats` | — | ❌ | TTS/music-specific. |
| `default_format` | — | ❌ | TTS/music-specific. |
| `prompt_character_limit` | — | ❌ | Music-specific. |
| `min_prompt_length` | — | ❌ | Music-specific. |
| `lyrics_character_limit` | — | ❌ | Music-specific. |
| `supportsStyleReferences` | — | ❌ | Image-specific. |

## 3. `model_spec.capabilities` fields

| swagger field | SDK property | Status | Notes |
|---------------|--------------|--------|-------|
| `optimizedForCode` | `ModelCapabilitiesSpec.optimizedForCode` | ✅ | Not exposed as top-level `ModelCapabilities` property. |
| `quantization` | — | ❌ | Not parsed. |
| `supportsFunctionCalling` | `ModelCapabilitiesSpec.supportsFunctionCalling`, `ModelCapabilities.supportsToolCalling` | ✅ | Renamed to `supportsToolCalling` in `ModelCapabilities`. |
| `supportsAudioInput` | `ModelCapabilitiesSpec.supportsAudioInput`, `ModelCapabilities.supportsAudioInput` | ✅ | |
| `supportsReasoning` | `ModelCapabilitiesSpec.supportsReasoning`, `ModelCapabilities.supportsReasoning` | ✅ | |
| `supportsReasoningEffort` | `ModelCapabilitiesSpec.supportsReasoningEffort` | ✅ | Not exposed in `ModelCapabilities`. |
| `reasoningEffortOptions` | — | ❌ | Not parsed. |
| `defaultReasoningEffort` | — | ❌ | Not parsed. |
| `supportsResponseSchema` | `ModelCapabilitiesSpec.supportsResponseSchema`, `ModelCapabilities.supportsResponseSchema` | ✅ | |
| `supportsMultipleImages` | `ModelCapabilitiesSpec.supportsMultipleImages` | ✅ | Used only to derive `supportsImageInput`; not exposed standalone. |
| `maxImages` | — | ❌ | Not parsed. |
| `maxVideos` | — | ❌ | Not parsed. |
| `supportsVision` | `ModelCapabilitiesSpec.supportsVision`, `ModelCapabilities.supportsVision` | ✅ | |
| `supportsVideoInput` | `ModelCapabilitiesSpec.supportsVideoInput`, `ModelCapabilities.supportsVideoInput` | ✅ | |
| `supportsWebSearch` | `ModelCapabilitiesSpec.supportsWebSearch`, `ModelCapabilities.supportsWebSearch` | ✅ | |
| `supportsLogProbs` | `ModelCapabilitiesSpec.supportsLogProbs` | ✅ | Not exposed in `ModelCapabilities`. |
| `supportsTeeAttestation` | `ModelCapabilitiesSpec.supportsTeeAttestation` | ✅ | Not exposed in `ModelCapabilities`. |
| `supportsE2EE` | `ModelCapabilitiesSpec.supportsE2EE` | ✅ | Not exposed in `ModelCapabilities`. |
| `supportsXSearch` | `ModelCapabilitiesSpec.supportsXSearch`, `ModelCapabilities.supportsXSearch` | ✅ | |

## 4. `model_spec.constraints` fields

| swagger field | SDK property | Status | Notes |
|---------------|--------------|--------|-------|
| Image constraints (`aspectRatios`, `resolutions`, `qualities`, `steps`, etc.) | — | ❌ | Not parsed. |
| Text constraints (`temperature`, `top_p`, `frequency_penalty`, etc.) | — | ❌ | Not parsed. |
| Video constraints (`aspect_ratios`, `durations`, `model_type`, etc.) | — | ❌ | Not parsed. |
| Inpaint constraints (`maxInputImages`, `singleImageAspectRatio`, etc.) | — | ❌ | Not parsed. |

## 5. `/models/traits` and `/models/compatibility_mapping`

| swagger shape | SDK shape | Status | Notes |
|---------------|-------------|--------|-------|
| `{"data": {"<trait>": "<model-id>"}, ...}` | `Map<String, String>` | ✅ | Parsed correctly. |
| `type` query parameter (default `text`) | — | ❌ | Not sent; see SDK-CORE-01. |

## 6. Derived `ModelCapabilities` properties

| SDK property | Derivation | Authoritative? | Notes |
|--------------|------------|----------------|-------|
| `supportsImageInput` | `supportsVision \|\| supportsMultipleImages` | ⚠️ Partial | Reasonable heuristic. |
| `supportsStreaming` | `spec?.offline != true` | ⚠️ Partial | Uses `offline` as proxy; see SDK-CORE-04. |
| `supportsSystemPrompt` | hard-coded `true` | ❌ No | Not based on runtime data. |
| `supportsTextChat` | `type == "text" \|\| "code"` | ⚠️ Partial | `"code"` is not a swagger response type. |
| `supportsImageGeneration` | `type == "image"` | ⚠️ Partial | Misses `inpaint`/`upscale` image types. |
| `inputModalities` | text + image/video/audio based on capability flags | ✅ | Reasonable derivation. |
| `outputModalities` | text + image/video/audio based on `type` | ✅ | Misses ASR text output nuance. |
| `compatibleWith` | inverted compatibility map | ✅ | Computed from `/models/compatibility_mapping`. |

## 7. Summary statistics

- **Top-level `/models` fields covered:** 6 / 8 (75%)
- **`model_spec` fields covered:** ~18 / 50+ (~35%)
- **`model_spec.capabilities` fields covered:** 12 / 15 (80%)
- **`model_spec.constraints` covered:** 0%
- **Traits / compatibility shape:** 100% (but missing `type` query support)

The SDK currently targets a chat-centric subset of the Venice model metadata. Media-specific metadata (pricing, constraints, audio/video/music fields, deprecation) is largely unexposed, limiting capability-aware UI and request validation.
