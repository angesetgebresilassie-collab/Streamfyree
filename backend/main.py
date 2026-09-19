from __future__ import annotations

import asyncio
import re
import time
from collections import defaultdict
from typing import Any

import httpx
import yt_dlp
from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.responses import RedirectResponse

app = FastAPI(title="Streamfyree API", version="0.2.0")

ITUNES = "https://itunes.apple.com/search"
RESOLVE_TTL_SECONDS = 8 * 60
RESOLVE_TIMEOUT_SECONDS = 25
SEARCH_TIMEOUT_SECONDS = 30
RATE_WINDOW_SECONDS = 60
RATE_LIMIT = 60

_resolve_cache: dict[str, tuple[float, dict[str, Any]]] = {}
_rate_buckets: dict[str, list[float]] = defaultdict(list)
_resolve_lock = asyncio.Lock()


def _clean(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", " ", value.lower()).strip()


def _score_match(title: str, artist: str, meta: dict[str, Any]) -> int:
    yt_title = _clean(title)
    yt_artist = _clean(artist)
    track = _clean(str(meta.get("trackName", "")))
    meta_artist = _clean(str(meta.get("artistName", "")))
    score = 0
    if track and track in yt_title:
        score += 5
    if meta_artist and meta_artist in yt_title:
        score += 4
    if meta_artist and meta_artist in yt_artist:
        score += 3
    if track and meta_artist and f"{meta_artist} {track}" in yt_title:
        score += 4
    return score


def youtube_search(query: str) -> list[dict[str, Any]]:
    opts = {
        "quiet": True,
        "skip_download": True,
        "extract_flat": True,
        "noplaylist": True,
        "socket_timeout": SEARCH_TIMEOUT_SECONDS,
    }
    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info(f"ytsearch8:{query} lyric video", download=False)
    return info.get("entries", [])


async def itunes(query: str) -> list[dict[str, Any]]:
    timeout = httpx.Timeout(10.0)
    async with httpx.AsyncClient(timeout=timeout) as client:
        response = await client.get(
            ITUNES,
            params={"term": query, "media": "music", "limit": 12},
        )
        response.raise_for_status()
        return response.json().get("results", [])


async def _rate_limit(request: Request) -> None:
    address = request.client.host if request.client else "unknown"
    now = time.monotonic()
    bucket = [t for t in _rate_buckets[address] if now - t < RATE_WINDOW_SECONDS]
    if len(bucket) >= RATE_LIMIT:
        raise HTTPException(429, "Rate limit exceeded; try again shortly")
    bucket.append(now)
    _rate_buckets[address] = bucket


def _search_sync(query: str) -> list[dict[str, Any]]:
    return youtube_search(query)


async def _resolve_uncached(video_id: str) -> dict[str, Any]:
    url = f"https://www.youtube.com/watch?v={video_id}"
    opts = {
        "quiet": True,
        "skip_download": True,
        "format": "bestaudio[abr<=128]/bestaudio",
        "noplaylist": True,
        "socket_timeout": RESOLVE_TIMEOUT_SECONDS,
    }
    try:
        info = await asyncio.wait_for(
            asyncio.to_thread(_extract_video, url, opts),
            timeout=RESOLVE_TIMEOUT_SECONDS + 3,
        )
    except asyncio.TimeoutError as exc:
        raise HTTPException(504, "Stream resolution timed out") from exc
    except Exception as exc:
        raise HTTPException(502, f"yt-dlp resolution failed: {exc}") from exc

    stream_url = info.get("url")
    if not stream_url:
        raise HTTPException(502, "No playable audio stream was returned")

    return {
        "id": video_id,
        "title": info.get("title", ""),
        "artist": info.get("uploader", ""),
        "album": "",
        "artwork": info.get("thumbnail", ""),
        "youtube_url": url,
        "stream_url": stream_url,
        "duration_ms": int((info.get("duration") or 0) * 1000),
        "expires_at": time.time() + RESOLVE_TTL_SECONDS,
    }


def _extract_video(url: str, opts: dict[str, Any]) -> dict[str, Any]:
    with yt_dlp.YoutubeDL(opts) as ydl:
        return ydl.extract_info(url, download=False)


async def _resolve_cached(video_id: str) -> dict[str, Any]:
    cached = _resolve_cache.get(video_id)
    if cached and cached[0] > time.time():
        return cached[1]

    async with _resolve_lock:
        cached = _resolve_cache.get(video_id)
        if cached and cached[0] > time.time():
            return cached[1]
        resolved = await _resolve_uncached(video_id)
        _resolve_cache[video_id] = (
            time.time() + RESOLVE_TTL_SECONDS,
            resolved,
        )
        return resolved


@app.get("/api/health")
async def health() -> dict[str, Any]:
    return {
        "ok": True,
        "service": "streamfyree-api",
        "cached_resolutions": len(_resolve_cache),
    }


@app.get("/api/search")
async def search(request: Request, q: str = Query(min_length=1, max_length=120)):
    await _rate_limit(request)
    try:
        yt, meta = await asyncio.gather(
            asyncio.to_thread(_search_sync, q),
            itunes(q),
        )
    except Exception as exc:
        raise HTTPException(502, f"Search failed: {exc}") from exc

    results: list[dict[str, Any]] = []
    for item in yt:
        video_id = item.get("id")
        if not video_id:
            continue
        title = item.get("title") or ""
        channel = item.get("channel") or item.get("uploader") or ""
        match = max(meta, key=lambda m: _score_match(title, channel, m), default={})
        score = _score_match(title, channel, match)
        artwork = str(match.get("artworkUrl100", "")).replace("100x100", "600x600")
        results.append(
            {
                "id": video_id,
                "title": title,
                "artist": match.get("artistName") or channel,
                "album": match.get("collectionName") or "",
                "artwork": artwork,
                "youtube_url": item.get("webpage_url")
                or f"https://www.youtube.com/watch?v={video_id}",
                "lyric_video": "lyric" in title.lower(),
                "match_score": score,
                "duration_ms": int((item.get("duration") or 0) * 1000),
            }
        )
    results.sort(key=lambda x: (not x["lyric_video"], -x["match_score"]))
    return results


@app.get("/api/track/{video_id}")
async def track(video_id: str, request: Request):
    await _rate_limit(request)
    results = await search(request, video_id)
    match = next((item for item in results if item["id"] == video_id), None)
    if match:
        return match
    raise HTTPException(404, "Track not found")


@app.get("/api/resolve/{video_id}")
async def resolve(video_id: str, request: Request):
    await _rate_limit(request)
    return await _resolve_cached(video_id)


@app.get("/api/stream/{video_id}")
async def stream(video_id: str, request: Request):
    resolved = await resolve(video_id, request)
    return RedirectResponse(resolved["stream_url"], status_code=307)
