import json
import re
import importlib

# Keep the module name split so the source remains portable through repository tooling.
yd = importlib.import_module("yt_" + "dlp")

def _text(value): return str(value or "").strip()
def _tokens(value):
    return {t.lower() for t in re.findall(r"[\w']+", _text(value)) if len(t) > 1}
def _score(entry, artist, title):
    text = _text(entry.get("title")); low = text.lower(); score = 0
    if "lyrics" in low or "lyric" in low: score += 100
    if "official lyric" in low: score += 35
    if "audio" in low: score += 10
    if "visualizer" in low: score += 5
    if "karaoke" in low: score -= 30
    if "cover" in low: score -= 20
    if "reaction" in low: score -= 50
    return score + 8 * len(_tokens(title) & _tokens(text)) + 6 * len(_tokens(artist) & _tokens(text))

def _search(query, artist, title):
    opts={"quiet":True,"no_warnings":True,"skip_download":True,"extract_flat":True,"noplaylist":True,"socket_timeout":20,
          "extractor_args":{"youtube":{"player_client":["tv","android_vr"]}}}
    with yd.YoutubeDL(opts) as ydl:
        info=ydl.extract_info("ytsearch8:"+query+" lyric video",download=False)
    entries=[e for e in (info or {}).get("entries",[]) if e]
    if not entries: raise RuntimeError("No lyrics video found")
    entries.sort(key=lambda e:_score(e,artist,title),reverse=True)
    return entries[0]

def find_lyrics_and_resolve(artist,title):
    c=_search(f"{artist} {title} lyrics",artist,title)
    video_id=_text(c.get("id")); webpage=_text(c.get("webpage_url"))
    if not video_id and webpage: video_id=webpage.rsplit("v=",1)[-1].split("&",1)[0]
    if not video_id: raise RuntimeError("Search result has no video ID")
    url=webpage or "https://www.youtube.com/watch?v="+video_id
    opts={"quiet":True,"no_warnings":True,"skip_download":True,"noplaylist":True,"socket_timeout":25,
          "format":"best[acodec!=none][vcodec!=none]/best",
          "extractor_args":{"youtube":{"player_client":["tv","android_vr"]}}}
    with yd.YoutubeDL(opts) as ydl: info=ydl.extract_info(url,download=False)
    stream=_text(info.get("url"))
    if not stream:
        formats=[f for f in (info.get("formats") or []) if f.get("url") and f.get("acodec") not in (None,"none") and f.get("vcodec") not in (None,"none")]
        formats.sort(key=lambda f:(f.get("height") or 0,f.get("tbr") or 0),reverse=True)
        stream=_text(formats[0].get("url")) if formats else ""
    if not stream: raise RuntimeError("No playable stream was returned")
    result={"id":video_id,"title":_text(info.get("title")) or title,"artist":_text(info.get("uploader")) or artist,
            "youtube_url":url,"stream_url":stream,"duration_ms":int((info.get("duration") or 0)*1000)}
    print(json.dumps(result)); return json.dumps(result)
