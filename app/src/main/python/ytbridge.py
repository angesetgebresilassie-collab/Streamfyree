import json
import re
import yt_dlp


def _text(value):
    return str(value or "").strip()


def _tokens(value):
    return {
        token.lower()
        for token in re.findall(r"[\w']+", _text(value))
        if len(token) > 1
    }


def _score(entry, artist, title):
    text = _text(entry.get("title"))
    low = text.lower()
    score = 0

    if "lyrics" in low or "lyric" in low:
        score += 100
    if "official lyric" in low:
        score += 35
    if "audio" in low:
        score += 10
    if "visualizer" in low:
        score += 5
    if "karaoke" in low:
        score -= 30
    if "cover" in low:
        score -= 20
    if "reaction" in low:
        score -= 50

    score += 8 * len(_tokens(title) & _tokens(text))
    score += 6 * len(_tokens(artist) & _tokens(text))
    return score


def _search(query, artist, title):
    opts = {
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "extract_flat": True,
        "noplaylist": True,
        "socket_timeout": 20,
        "extractor_args": {
            "youtube": {
                "player_client": ["tv", "android_vr"],
            }
        },
    }
    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info("ytsearch8:" + query + " lyric video", download=False)

    entries = [e for e in (info or {}).get("entries", []) if e]
    if not entries:
        raise RuntimeError("No YouTube lyrics video found")

    entries.sort(key=lambda e: _score(e, artist, title), reverse=True)
    return entries[0]


def find_lyrics_and_resolve(artist, title):
    query = f"{artist} {title} lyrics"
    candidate = _search(query, artist, title)
    video_id = _text(candidate.get("id"))
    webpage_url = _text(candidate.get("webpage_url"))

    if not video_id and webpage_url:
        video_id = webpage_url.rsplit("v=", 1)[-1].split("&", 1)[0]
    if not video_id:
        raise RuntimeError("YouTube returned a result without a video ID")

    url = webpage_url or "https://www.youtube.com/watch?v=" + video_id

    opts = {
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "noplaylist": True,
        "socket_timeout": 25,
        "format": "bestaudio[acodec!=none]/bestaudio",
        "extractor_args": {
            "youtube": {
                "player_client": ["tv", "android_vr"],
            }
        },
    }

    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info(url, download=False)

    stream_url = _text(info.get("url"))
    if not stream_url:
        formats = [
            f for f in (info.get("formats") or [])
            if f.get("url") and (f.get("acodec") not in (None, "none"))
        ]
        formats.sort(key=lambda f: (f.get("abr") or 0, f.get("tbr") or 0), reverse=True)
        stream_url = _text(formats[0].get("url")) if formats else ""

    if not stream_url:
        raise RuntimeError("yt-dlp found the lyrics video but no playable audio stream")

    print(json.dumps({
        "id": video_id,
        "title": _text(info.get("title")) or title,
        "artist": _text(info.get("uploader")) or artist,
        "youtube_url": url,
        "stream_url": stream_url,
        "duration_ms": int((info.get("duration") or 0) * 1000),
    }))
    return json.dumps({
        "id": video_id,
        "title": _text(info.get("title")) or title,
        "artist": _text(info.get("uploader")) or artist,
        "youtube_url": url,
        "stream_url": stream_url,
        "duration_ms": int((info.get("duration") or 0) * 1000),
    })
