# Streamfyree backend

The Android app never runs yt-dlp itself. This service performs YouTube discovery and server-side yt-dlp resolution, then returns a playable media URL.

Run:
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8080

Set the Android STREAM_API_BASE build field to http://YOUR_SERVER:8080/api/.
The resolver prefers audio streams at or below 128 kbps and search prefers lyric-video results.
