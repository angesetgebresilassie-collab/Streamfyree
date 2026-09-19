# Streamfyree

Colorful, frosted-glass Android music player backed by server-side yt-dlp and iTunes metadata.

Android: Kotlin, Jetpack Compose, Media3.
Discovery: YouTube search through yt-dlp on the backend.
Metadata/artwork: iTunes Search API.
Native mode: backend resolves an audio URL with yt-dlp; Media3 plays it.
YouTube mode: the track URL is retained for an embedded player integration.
Bandwidth: backend prefers audio streams at or below 128 kbps.

Development:
1. Run backend/ using backend/README.md.
2. Set STREAM_API_BASE in app/build.gradle.kts.
3. Build with Gradle or Android Studio.

YouTube and yt-dlp usage must comply with applicable service terms and copyright laws.
