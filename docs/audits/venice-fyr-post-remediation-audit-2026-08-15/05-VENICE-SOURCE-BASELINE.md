# Venice Source Baseline

- Refreshed with `./scripts/bootstrap-venice-api-docs.sh`.
- Upstream: `https://github.com/veniceai/api-docs.git`, branch `main`.
- HEAD: `6e69346b13695bd53ba33a1d34e7b28841e10f98`.
- Swagger `info.version`: `20260814.194349`.
- Drift from repository baseline: none.

Consulted: `swagger.yaml`, `agents.md`, `skill.md`, `api-reference/endpoint/chat`, `api-reference/endpoint/image`, `api-reference/endpoint/video`, `api-reference/endpoint/audio`, `guides/features/reasoning-models.mdx`, media guides, `overview/`, and `data/static-models.json`.

Contract conclusions: edit/multi-edit/upscale are binary images; video retrieve can return JSON `PROCESSING` or `COMPLETED` or binary MP4; video queue alone may return `download_url`; chat reasoning is top-level request state and separate streamed `reasoning_content`; queued audio has quote/queue/retrieve/complete and binary completion; transcription/voice operations are multipart.
