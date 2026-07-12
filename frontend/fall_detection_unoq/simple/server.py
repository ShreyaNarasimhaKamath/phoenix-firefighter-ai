"""
AI PC server — receives firefighter events from the UNO Q and shows a
live heat-map table in the browser plus a live feed in this terminal.

Run on the AI PC (IP 10.91.62.10):
    pip install flask
    python server.py

Then open http://10.91.62.10:5000 in a browser for the heat-map table.
"""

from flask import Flask, request, jsonify
from datetime import datetime
import json

app = Flask(__name__)
LOG_FILE = "events.jsonl"
recent = []          # last N events kept in memory for the dashboard
MAX_RECENT = 30


@app.route("/event", methods=["POST"])
def event():
    data = request.get_json(force=True, silent=True) or {}
    data["received_at"] = datetime.now().strftime("%H:%M:%S")

    recent.append(data)
    if len(recent) > MAX_RECENT:
        recent.pop(0)

    temp = data.get("temp", {}).get("celsius")
    temp_str = f"{temp:.1f}C" if isinstance(temp, (int, float)) else "--"
    decision = data.get("decision", "?")
    line = (f"[{data['received_at']}] {data.get('posture', '?'):9s} | "
            f"gas {data.get('gas', {}).get('raw', '?')} | temp {temp_str} | {decision}")
    if decision != "NORMAL":
        line += "   <<<<<<<<"
    print(line, flush=True)

    with open(LOG_FILE, "a") as f:
        f.write(json.dumps(data) + "\n")

    return jsonify(ok=True)


def cell(value, alert, warn=False):
    color = "#e74c3c" if alert else ("#f39c12" if warn else "#2ecc71")
    return f'<td style="background:{color};color:#fff;padding:6px 10px">{value}</td>'


@app.route("/", methods=["GET"])
def dashboard():
    rows = ""
    for d in reversed(recent):
        posture = d.get("posture", "?")
        fell = posture == "FELL DOWN"
        gas = d.get("gas", {})
        temp = d.get("temp", {})
        acc = d.get("accel", {})
        temp_val = temp.get("celsius")
        temp_str = f"{temp_val:.1f}" if isinstance(temp_val, (int, float)) else "--"
        decision = d.get("decision", "?")
        rows += "<tr>"
        rows += f'<td style="padding:6px 10px">{d.get("received_at", "")}</td>'
        rows += cell(posture, fell)
        rows += f'<td style="padding:6px 10px">{acc.get("magnitude", "?")}</td>'
        rows += cell(gas.get("raw", "?"), gas.get("alert", False))
        rows += cell(temp_str, temp.get("alert", False))
        rows += cell(decision, decision not in ("NORMAL", "?"),
                     warn=decision in ("GAS WARNING", "HEAT WARNING"))
        rows += "</tr>"

    return f"""<!doctype html>
<html><head><title>Firefighter Monitor</title>
<meta http-equiv="refresh" content="1">
<style>body{{font-family:monospace;background:#1e1e1e;color:#eee;padding:20px}}
table{{border-collapse:collapse}} th{{padding:6px 10px;background:#333;text-align:left}}
td{{border:1px solid #444}}</style></head>
<body>
<h2>Firefighter Monitor — live ({len(recent)} events)</h2>
<table>
<tr><th>Time</th><th>Posture</th><th>Accel mag (m/s&sup2;)</th><th>Gas</th><th>Temp (&deg;C)</th><th>Decision</th></tr>
{rows}
</table>
</body></html>"""


if __name__ == "__main__":
    print("=" * 55)
    print("Firefighter server on port 5000")
    print("Heat-map table: http://10.91.62.10:5000")
    print("=" * 55)
    app.run(host="0.0.0.0", port=5000)
