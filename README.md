# Streamfyree

A premium Android music player with a Spotify-inspired frosted-glass interface.

### What it does

- Real music discovery using iTunes metadata and artwork
- Randomized home feed with fresh songs
- Search results displayed as real, clickable tracks
- On-device YouTube lyrics-video discovery with embedded yt-dlp
- On-device yt-dlp stream resolution with Media3 playback
- Queue, library, favorites, recently played, and background playback
- Media3 controls for lock screen, Bluetooth, and headset playback
- Low-bandwidth audio selection
- Color-aware artwork styling

### Architecture

Everything runs from the Android app. iTunes supplies music metadata and artwork. Chaquopy embeds Python and yt-dlp directly in the APK. When a track is played, the app searches YouTube for a matching lyrics video, resolves a playable media URL locally, and hands that URL to Media3.

There is no Streamfyree API server, no STREAM_API_BASE, and no separate backend deployment.

> YouTube and yt-dlp usage must comply with applicable service terms and copyright laws.
