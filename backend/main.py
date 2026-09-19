from typing import Any
import httpx
import yt_dlp
from fastapi import FastAPI, HTTPException, Query
from fastapi.responses import RedirectResponse

app=FastAPI(title="Streamfyree API")
ITUNES="https://itunes.apple.com/search"

def youtube_search(query:str)->list[dict[str,Any]]:
    opts={"quiet":True,"skip_download":True,"extract_flat":True,"noplaylist":True}
    with yt_dlp.YoutubeDL(opts) as ydl:
        info=ydl.extract_info("ytsearch8:"+query+" lyric video",download=False)
    return info.get("entries",[])

async def itunes(query:str)->list[dict[str,Any]]:
    async with httpx.AsyncClient(timeout=10) as client:
        r=await client.get(ITUNES,params={"term":query,"media":"music","limit":8})
        r.raise_for_status()
        return r.json().get("results",[])

@app.get("/api/search")
async def search(q:str=Query(min_length=1)):
    yt=youtube_search(q); meta=await itunes(q)
    results=[]
    for item in yt:
        title=item.get("title") or ""; channel=item.get("channel") or item.get("uploader") or ""
        match=next((m for m in meta if m.get("artistName","").lower() in title.lower() or m.get("trackName","").lower() in title.lower()),None)
        results.append({"id":item.get("id"),"title":title,"artist":match.get("artistName",channel) if match else channel,"album":match.get("collectionName","") if match else "","artwork":match.get("artworkUrl100","").replace("100x100","600x600") if match else "","youtube_url":item.get("webpage_url") or "https://www.youtube.com/watch?v="+str(item.get("id")),"lyric_video":"lyric" in title.lower()})
    return results

@app.get("/api/resolve")
async def resolve(id:str):
    url="https://www.youtube.com/watch?v="+id
    opts={"quiet":True,"skip_download":True,"format":"bestaudio[abr<=128]/bestaudio","noplaylist":True}
    try:
        with yt_dlp.YoutubeDL(opts) as ydl: info=ydl.extract_info(url,download=False)
        return {"id":id,"title":info.get("title",""),"artist":info.get("uploader",""),"youtube_url":url,"stream_url":info.get("url"),"duration_ms":int((info.get("duration") or 0)*1000)}
    except Exception as exc:
        raise HTTPException(502,"yt-dlp resolution failed: "+str(exc))

@app.get("/api/stream")
async def stream(id:str):
    resolved=await resolve(id)
    if not resolved.get("stream_url"): raise HTTPException(502,"No playable stream")
    return RedirectResponse(resolved["stream_url"])
