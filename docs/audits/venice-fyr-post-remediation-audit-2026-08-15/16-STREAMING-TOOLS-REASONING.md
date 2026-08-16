# Streaming, Tools, and Reasoning

Verified by source/tests: incremental SSE reads, LF/CRLF handling through `BufferedReader`, multiline `data:` joins, comments and `event`/`id`/`retry` ignored as control fields, `[DONE]`, explicit `finish_reason`, strict early EOF failure, multiple/fragmented tool calls, redacted provider error chunks, non-2xx structured exceptions, and consumer cancellation reaching the OkHttp call.

This audit adds separate `ReasoningDelta` events, typed `reasoning`/`reasoning_effort`, accumulator separation, interleaving with content/tools, and exact encrypted placeholder preservation. Reasoning is not merged into answer text.

Open: stream usage, web-search result metadata, non-streaming JSON completion, multimodal message unions, native search tool definitions, structured response formats, and app persistence/rendering of reasoning.
