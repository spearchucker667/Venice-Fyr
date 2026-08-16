# Media Jobs

Image edit/multi-edit/upscale now validate image MIME and non-empty binary bodies and return bytes with request/balance metadata. The app edit path consumes bytes directly.

Video retrieve now models processing, remote JSON completion, unknown JSON status, and binary completion. The queue response owns an optional pre-signed download URL. Video quote, complete success, and transcription are typed.

Audio now models quote, queue submission, processing, binary completion, unknown status, cleanup success, multipart transcription, and multipart voice cloning. No helper treats queue submission as completion.

Still absent: quote/approval UI, durable jobs, bounded polling, WorkManager, duplicate idempotency state, download verification, durable media storage, cleanup retry, export/share, and large-file controls.
