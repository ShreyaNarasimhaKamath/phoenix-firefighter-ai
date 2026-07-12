"""
AI PC receiver for the hackathon demo.

Receives detection events from BOTH Android apps:
  - Sound Classifier  -> source: "audio"  (emergency sound, severity, proximity)
  - Object Detection  -> source: "vision" (detected objects + scores)

Run on the AI PC:
    pip install flask
    python server.py

Then open http://localhost:5000 for the live dashboard.
Phones must be on the SAME Wi-Fi and point at this PC's IP
(find it with `ipconfig` -> IPv4 Address, e.g. 192.168.1.42).
"""

import json
import time
from collections import deque
from flask import Flask, request, jsonify

app = Flask(__name__)

EVENTS = deque(maxlen=200)   # newest last
LOG_FILE = "events.jsonl"    # every event is also appended here (one JSON per line)


@app.post("/event")
def receive_event():
    data = request.get_json(force=True, silent=True) or {}
    data["received_at"] = time.strftime("%H:%M:%S")
    EVENTS.append(data)
    print_event(data)
    with open(LOG_FILE, "a", encoding="utf-8") as f:
        f.write(json.dumps(data) + "\n")
    return jsonify(ok=True)


def print_event(e):
    """One readable line per event, in real time."""
    t = e.get("detected_at", e["received_at"])
    if e.get("source") == "audio":
        print(f"[{t}] AUDIO  | {e.get('label','?'):<25} "
              f"{e.get('severity',''):<8} {round(e.get('score',0)*100):>3}%  "
              f"{e.get('proximity','')} ({e.get('proximity_range','')})")
    elif e.get("source") == "vision":
        objs = ", ".join(f"{o['label']} {round(o['score']*100)}%"
                         for o in e.get("objects", []))
        print(f"[{t}] VISION | {objs}")
    else:
        print(f"[{t}] ?????? | {e}")


@app.get("/events")
def list_events():
    return jsonify(list(EVENTS)[::-1])   # newest first


@app.get("/")
def dashboard():
    return """<!doctype html>
<html><head><title>AI PC — Live Detections</title>
<style>
 body{font-family:system-ui,sans-serif;background:#0f1115;color:#e8e8e8;margin:0;padding:24px}
 h1{font-size:20px} .cols{display:flex;gap:24px;flex-wrap:wrap}
 .col{flex:1;min-width:320px}
 .card{background:#1a1e26;border-radius:10px;padding:10px 14px;margin-bottom:8px;
       border-left:4px solid #555}
 .audio{border-left-color:#e05555}.vision{border-left-color:#4d9de0}
 .label{font-weight:600}.meta{color:#9aa0aa;font-size:13px}
 .CRITICAL{color:#ff6b6b}.HIGH{color:#ffb056}.MEDIUM{color:#ffd93d}
</style></head><body>
<h1>🖥️ AI PC — Live Detections</h1>
<div class="cols">
 <div class="col"><h2>🔊 Audio (Sound Classifier)</h2><div id="audio"></div></div>
 <div class="col"><h2>📷 Vision (Object Detection)</h2><div id="vision"></div></div>
</div>
<script>
async function tick(){
  const r = await fetch('/events'); const evs = await r.json();
  for (const src of ['audio','vision']){
    const el = document.getElementById(src);
    el.innerHTML = evs.filter(e=>e.source===src).slice(0,25).map(e=>`
      <div class="card ${src}">
        <span class="label">${e.label??'?'}</span>
        <span class="${e.severity??''}">${e.severity??''}</span>
        <div class="meta">
          ${e.score!==undefined?Math.round(e.score*100)+'% conf':''}
          ${e.proximity?(' • '+e.proximity+' ('+(e.proximity_range??'')+')'):''}
          ${e.objects?(' • '+e.objects.map(o=>o.label+' '+Math.round(o.score*100)+'%').join(', ')):''}
          • detected ${e.detected_at??'?'} • received ${e.received_at}
        </div>
      </div>`).join('');
  }
}
setInterval(tick, 1000); tick();
</script></body></html>"""


if __name__ == "__main__":
    # host 0.0.0.0 so phones on the Wi-Fi can reach it
    app.run(host="0.0.0.0", port=5000)
