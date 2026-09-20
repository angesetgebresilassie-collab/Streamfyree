# Streamfyree

A premium Android music player with a Spotify-inspired frosted-glass interface.

### What it does

- Real music discovery using iTunes metadata and artwork
- Randomized home feed with fresh songs
- Search results displayed as real, clickable tracks
- Native Audio playback through the Streamfyree backend
- Online playback with lyric-video discovery
- Queue, library, favorites, recently played, and background playback
- Media3 controls for lock screen, Bluetooth, and headset playback
- Low-bandwidth audio selection
- Color-aware artwork styling

### Architecture

The Android app handles the interface and playback controls. The optional backend handles YouTube discovery and server-side media resolution so yt-dlp is not bundled into the Android APK.

### Running the backend

The backend lives in `backend/` and exposes the API consumed by the Android app. See `backend/README.md` for server setup.

### Android build

Open the project in Android Studio or build the `app` module with Gradle. The app can use a configured `STREAM_API_BASE` value for the backend URL.

> YouTube and yt-dlp usage must comply with applicable service terms and copyright laws.
