# Streamfyree Music App

This branch is the dedicated working branch for the Streamfyree music-streaming app.

## Direction
- Native Android playback with an embedded Python stream resolver
- iTunes artwork and metadata
- YouTube lyrics-video discovery
- Media3 background playback
- Colorful frosted-glass-inspired interface

## Playback architecture
Chaquopy embeds Python and the stream resolver inside the APK. The app searches for a matching lyrics video, resolves a playable media URL locally, and gives that URL to Media3.

YouTube and the stream resolver must be used in accordance with applicable service terms and copyright laws.
