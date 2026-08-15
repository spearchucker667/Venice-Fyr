# Venice API Port Matrix

Generated from the desktop tracked OpenAPI snapshot `20260814.153445`. This table is an inventory, not permission to expose every endpoint in the app UI.

| Path | Methods | Android SDK status |
|---|---|---|
| `/chat/completions` | POST | Planned typed SDK method |
| `/responses` | POST | Planned typed SDK method |
| `/image/generate` | POST | Planned typed SDK method |
| `/images/generations` | POST | Planned typed SDK method |
| `/image/styles` | GET | Planned typed SDK method |
| `/image/upscale` | POST | Planned typed SDK method |
| `/image/edit` | POST | Planned typed SDK method |
| `/image/multi-edit` | POST | Planned typed SDK method |
| `/image/background-remove` | POST | Planned typed SDK method |
| `/models` | GET | Foundation: GET listModels() |
| `/models/traits` | GET | Planned typed SDK method |
| `/models/compatibility_mapping` | GET | Planned typed SDK method |
| `/api_keys` | GET, DELETE, POST, PATCH | Planned typed SDK method |
| `/api_keys/{id}` | GET | Planned typed SDK method |
| `/api_keys/rate_limits` | GET | Planned typed SDK method |
| `/api_keys/rate_limits/log` | GET | Planned typed SDK method |
| `/api_keys/generate_web3_key` | GET, POST | Planned typed SDK method |
| `/characters` | GET | Planned typed SDK method |
| `/characters/{slug}` | GET | Planned typed SDK method |
| `/characters/{slug}/reviews` | GET | Planned typed SDK method |
| `/embeddings` | POST | Planned typed SDK method |
| `/audio/speech` | POST | Planned typed SDK method |
| `/audio/transcriptions` | POST | Planned typed SDK method |
| `/audio/voices` | POST | Planned typed SDK method |
| `/video/complete` | POST | Planned typed SDK method |
| `/video/queue` | POST | Planned typed SDK method |
| `/video/quote` | POST | Planned typed SDK method |
| `/video/retrieve` | POST | Planned typed SDK method |
| `/video/transcriptions` | POST | Planned typed SDK method |
| `/augment/text-parser` | POST | Planned typed SDK method |
| `/audio/complete` | POST | Planned typed SDK method |
| `/audio/queue` | POST | Planned typed SDK method |
| `/audio/quote` | POST | Planned typed SDK method |
| `/audio/retrieve` | POST | Planned typed SDK method |
| `/billing/balance` | GET | Planned typed SDK method |
| `/billing/usage` | GET | Planned typed SDK method |
| `/billing/usage-analytics` | GET | Planned typed SDK method |
| `/billing/usage-history` | GET | Planned typed SDK method |
| `/crypto/rpc/networks` | GET | Planned typed SDK method |
| `/crypto/rpc/{network}` | POST | Planned typed SDK method |
| `/x402/balance/{walletAddress}` | GET | Planned typed SDK method |
| `/x402/top-up` | POST | Planned typed SDK method |
| `/x402/transactions/{walletAddress}` | GET | Planned typed SDK method |
| `/augment/scrape` | POST | Planned typed SDK method |
| `/augment/search` | POST | Planned typed SDK method |

## Implementation rules

- Add request/response fixtures and error normalization before marking an endpoint complete.
- Use live `/models`, `/models/traits`, and `/models/compatibility_mapping` data for runtime capability gating.
- Media queue endpoints require reconciliation/idempotency rules before automatic retry.
- Administrative/API-key, crypto, and x402 endpoints are part of the upstream wire contract but should only be surfaced if the Android product explicitly adopts the corresponding desktop/product behavior.
- Refresh this matrix whenever the tracked upstream OpenAPI snapshot changes.
