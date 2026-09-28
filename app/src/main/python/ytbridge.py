# Removed: this Python module (and the yt-dlp dependency it wrapped) is no
# longer used for playback resolution. NewPipeBridge.kt now performs YouTube
# search + audio-stream resolution natively in Kotlin. The Chaquopy Gradle
# plugin has also been removed from app/build.gradle.kts, so this file is no
# longer packaged into the app at all.
#
# Kept as a stub because the tools available here can create/update repo
# files but not delete them; safe to delete by hand along with the rest of
# app/src/main/python/.
