# Streamfyree Music App

This branch is the dedicated working branch for the Streamfyree music-streaming app. Treat `music-app` as the working main for this project.

## Direction
- Native yt-dlp audio streaming directly on Android
- iTunes artwork and metadata
- YouTube lyrics-video discovery on-device
- Media3 background playback
- Spotify-inspired, colorful frosted-glass interface

## Playback architecture
Chaquopy embeds Python and yt-dlp inside the APK. The app searches YouTube for a matching lyrics video, resolves the playable media URL locally, and gives it to Media3. There is no separate API server or backend deployment.
