package io.github.spearchucker667.veniceforge.android.feature

enum class FeatureGroup(val label: String) {
    CONVERSATION("Conversation"),
    GENERATE("Generate"),
    BUILD("Build"),
    SYSTEM("System"),
}

enum class AndroidPortStatus { FOUNDATION, SCAFFOLDED }

data class AppFeature(
    val id: String,
    val label: String,
    val group: FeatureGroup,
    val status: AndroidPortStatus,
    val desktopPurpose: String,
    val androidPortNotes: String,
)

object FeatureCatalog {
    val all = listOf(
        AppFeature("chat", "Chat", FeatureGroup.CONVERSATION, AndroidPortStatus.SCAFFOLDED, "Streaming conversations, projects, prompt injects, attachments, classical/agent modes", "Port SSE streaming, tool calls, attachments, project/chat persistence, model pinning, memory disclosure."),
        AppFeature("character-chats", "Character Chats", FeatureGroup.CONVERSATION, AndroidPortStatus.SCAFFOLDED, "Conversation-scoped hosted/local character chats", "Preserve isolated character identity, greetings, personas, lorebooks, and per-chat model state."),
        AppFeature("history", "History", FeatureGroup.CONVERSATION, AndroidPortStatus.SCAFFOLDED, "Browse, restore, organize, and inspect conversation state", "Back with Room; preserve folders, import/export, lock state, and recovery journals."),
        AppFeature("image", "Image Studio", FeatureGroup.GENERATE, AndroidPortStatus.SCAFFOLDED, "Generation, edit, multi-edit, background removal, upscale", "Use runtime model capabilities; preserve explicit provider safe_mode semantics and media integrity checks."),
        AppFeature("media", "Media Studio", FeatureGroup.GENERATE, AndroidPortStatus.SCAFFOLDED, "Gallery, comparison, lineage, metadata exports", "Use content-addressed app storage + SAF export; preserve source/action capability decoupling from the latest desktop fix."),
        AppFeature("image-inspector", "Image Inspector", FeatureGroup.GENERATE, AndroidPortStatus.SCAFFOLDED, "Vision analysis and prompt reconstruction", "Photo Picker/SAF input, bounded decoding, live vision-model pricing, structured result validation."),
        AppFeature("prompts", "Prompts", FeatureGroup.GENERATE, AndroidPortStatus.SCAFFOLDED, "Versioned/tagged prompt library", "Room-backed global/project scopes and immutable version chain."),
        AppFeature("scenes", "Scene Composer", FeatureGroup.GENERATE, AndroidPortStatus.SCAFFOLDED, "Arrange prompts, media references, and models", "Touch-first canvas with persisted node/edge geometry and media references."),
        AppFeature("audio", "Audio Studio", FeatureGroup.GENERATE, AndroidPortStatus.SCAFFOLDED, "TTS generation", "Queue/play/save audio with Media3 and SAF; retain voice/model metadata."),
        AppFeature("music", "Music Studio", FeatureGroup.GENERATE, AndroidPortStatus.SCAFFOLDED, "Lyrics-driven music generation", "Quote/queue/retrieve/complete pipeline with durable WorkManager state."),
        AppFeature("video", "Video Studio", FeatureGroup.GENERATE, AndroidPortStatus.SCAFFOLDED, "Async text/image-to-video", "Quote/queue/retrieve/complete; persist queue stages and resume after process death."),
        AppFeature("embeddings", "Embeddings", FeatureGroup.GENERATE, AndroidPortStatus.SCAFFOLDED, "Vector inspection/model evaluation", "Port request/response viewer without logging raw sensitive input."),
        AppFeature("search", "Research", FeatureGroup.GENERATE, AndroidPortStatus.SCAFFOLDED, "Search, scrape, synthesis, citations", "Port supported Venice/Jina workflow only; do not revive inactive embedded research browser."),
        AppFeature("characters", "Characters", FeatureGroup.GENERATE, AndroidPortStatus.SCAFFOLDED, "Hosted/local character library", "Preserve hosted/local split, image caching, card import/export, model handoff."),
        AppFeature("character-creator", "Character Creator", FeatureGroup.BUILD, AndroidPortStatus.SCAFFOLDED, "AI-assisted character authoring", "Keep immutable creator model contract unless desktop source-of-truth changes."),
        AppFeature("rp-studio", "RP Studio", FeatureGroup.BUILD, AndroidPortStatus.SCAFFOLDED, "Scenarios, personas, lorebooks, ST cards", "Port Character Card V1/V2 JSON and V2 PNG codec with bounded validation and compatibility fields."),
        AppFeature("workflows", "Workflows", FeatureGroup.BUILD, AndroidPortStatus.SCAFFOLDED, "Versioned automation chains", "Touch graph editor + executor; confirmations must remain explicit for mutating/paid operations."),
        AppFeature("documents", "Documents", FeatureGroup.BUILD, AndroidPortStatus.SCAFFOLDED, "Managed docs, revisions, workspace grants", "Map directory grants to SAF persisted tree URIs; no shell/Git/keychain/sibling-directory access."),
        AppFeature("playground", "Playground", FeatureGroup.BUILD, AndroidPortStatus.SCAFFOLDED, "Visual multi-model workflow execution", "Reuse workflow graph engine with mobile gestures and background task continuity."),
        AppFeature("privacy", "Privacy", FeatureGroup.SYSTEM, AndroidPortStatus.FOUNDATION, "Storage inventory, encrypted backup/sync, maintenance", "Keystore foundation present; add .vfbackup parity, profile purge, SAF sync folder, redacted inventory."),
        AppFeature("settings", "Config", FeatureGroup.SYSTEM, AndroidPortStatus.FOUNDATION, "Credentials, providers, language, themes, safety, sync", "API-key Keystore and live /models probe are implemented in starter; remaining panels are parity work."),
        AppFeature("status", "Status", FeatureGroup.SYSTEM, AndroidPortStatus.FOUNDATION, "Diagnostics, tasks, connectivity, rate limits, logs", "Starter reports source baseline; add redacted logs, background jobs, billing/rate-limit diagnostics."),
    )

    fun byId(id: String): AppFeature? = all.firstOrNull { it.id == id }
}
