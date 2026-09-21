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
                # Avoid android_vr: current YouTube GVS URLs from this
                # client can return HTTP 403 even for format 18.
                "player_client": ["web_embedded", "tv"],
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


# Keep this list limited to currently published Piped API endpoints.
# Dead instances can fail DNS resolution on Android and make the resolver
# appear completely broken even when another instance is healthy.
PIPED_INSTANCES = [
    "https://pipedapi.kavin.rocks",
    "https://pipedapi.tokhmi.xyz",
    "https://pipedapi.moomoo.me",
    "https://pipedapi.syncpundit.io",
    "https://api-piped.mha.fi",
    "https://piped-api.garudalinux.org",
    "https://pipedapi.leptons.xyz",
    "https://piped-api.privacy.com.de",
    "https://pipedapi.adminforge.de",
    "https://api.piped.yt",
]


def _http_json(url):
    import urllib.request
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "Streamfyree/1.0 (Android)",
            "Accept": "application/json",
        },
    )
    with urllib.request.urlopen(req, timeout=12) as response:
        return json.loads(response.read().decode("utf-8"))


def _piped_resolve(artist, title):
    import urllib.parse
    query = f"{artist} {title} lyrics"
    last_error = None

    for base in PIPED_INSTANCES:
        try:
            search_url = base + "/search?" + urllib.parse.urlencode({
                "q": query,
                "filter": "videos",
            })
            data = _http_json(search_url)
            items = [
                item for item in (data.get("items") or [])
                if item.get("type") == "stream" and item.get("url")
            ]
            if not items:
                continue

            items.sort(key=lambda item: _score({"title": _text(item.get("title"))}, artist, title), reverse=True)
            video_url = _text(items[0].get("url"))
            video_id = video_url.rsplit("v=", 1)[-1].split("&", 1)[0]
            if not video_id:
                continue

            stream_data = _http_json(base + "/streams/" + urllib.parse.quote(video_id, safe=""))
            streams = [
                stream for stream in (stream_data.get("audioStreams") or [])
                if stream.get("url") and not stream.get("videoOnly", False)
            ]
            if not streams:
                continue

            streams.sort(
                key=lambda x: (
                    1 if str(x.get("format", "")).upper() == "M4A" else 0,
                    x.get("bitrate") or 0,
                ),
                reverse=True,
            )
            stream = streams[0]
            return {
                "id": video_id,
                "title": _text(stream_data.get("title")) or _text(items[0].get("title")) or title,
                "artist": _text(stream_data.get("uploader")) or artist,
                "youtube_url": "https://www.youtube.com/watch?v=" + video_id,
                "stream_url": _text(stream.get("url")),
                "duration_ms": int((stream_data.get("duration") or 0) * 1000),
                "http_headers": {
                    "User-Agent": "Streamfyree/1.0 (Android)",
                    "Referer": "https://www.youtube.com/",
                },
            }
        except Exception as exc:
            last_error = exc

    if last_error:
        raise RuntimeError("YouTube resolver and all Piped fallbacks failed: " + str(last_error))
    raise RuntimeError("No playable audio stream found from YouTube/Piped")


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

    # YouTube changes client availability frequently. Try several clients
    # before touching Piped, and never let a dead Piped DNS entry hide the
    # actual YouTube resolver error.
    client_sets = [
        ["web_embedded", "tv"],
        ["web"],
        ["ios", "web"],
    ]
    info = None
    youtube_errors = []

    for clients in client_sets:
        opts = {
            "quiet": True,
            "no_warnings": True,
            "skip_download": True,
            "noplaylist": True,
            "socket_timeout": 25,
            "format": "bestaudio[ext=m4a]/bestaudio/best",
            "extractor_args": {
                "youtube": {
                    "player_client": clients,
                }
            },
        }
        try:
            with yt_dlp.YoutubeDL(opts) as ydl:
                info = ydl.extract_info(url, download=False)
            if info:
                break
        except Exception as exc:
            youtube_errors.append(f"{','.join(clients)}: {exc}")

    if not info:
        try:
            return json.dumps(_piped_resolve(artist, title))
        except Exception as piped_error:
            detail = youtube_errors[-1] if youtube_errors else "unknown YouTube error"
            raise RuntimeError(
                "YouTube extraction failed after all client profiles; "
                f"Piped fallback also failed: {piped_error}; last YouTube error: {detail}"
            ) from piped_error

    stream_url = _text(info.get("url"))
    if not stream_url:
        formats = [
            f for f in (info.get("formats") or [])
            if f.get("url") and f.get("acodec") not in (None, "none")
        ]
        formats.sort(
            key=lambda f: (
                1 if f.get("ext") == "m4a" else 0,
                f.get("abr") or 0,
                f.get("tbr") or 0,
            ),
            reverse=True,
        )
        stream_url = _text(formats[0].get("url")) if formats else ""

    if not stream_url:
        try:
            return json.dumps(_piped_resolve(artist, title))
        except Exception as piped_error:
            raise RuntimeError(
                "YouTube returned no playable stream; Piped fallback failed: "
                + str(piped_error)
            ) from piped_error

    http_headers = {
        str(key): str(value)
        for key, value in (info.get("http_headers") or {}).items()
        if value
    }

    return json.dumps({
        "id": video_id,
        "title": _text(info.get("title")) or title,
        "artist": _text(info.get("uploader")) or artist,
        "youtube_url": url,
        "stream_url": stream_url,
        "duration_ms": int((info.get("duration") or 0) * 1000),
        "http_headers": http_headers,
    })
