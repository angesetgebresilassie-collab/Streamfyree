package com.streamfyree.app

// Removed: the yt-dlp/Chaquopy resolution path. NewPipeBridge now performs
// both YouTube search and audio-stream resolution natively in Kotlin (see
// NewPipeBridge.searchAndResolve), so this bridge — and the bundled Python
// runtime it required — is no longer part of the build.
//
// This file is kept as an empty stub only because the tools available here
// can create/update repo files but not delete them; it's safe to delete by
// hand (`git rm app/src/main/java/com/streamfyree/app/YtDlpBridge.kt`).
