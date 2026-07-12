"""
AI PC event server — receives fall/gas/temp events from the Arduino UNO Q.

Run on the AI PC:
    pip install flask
    python server.py

Live events print in this terminal; everything is also logged to events.jsonl.
"""

from flask import Flask, request, jsonify
from datetime import datetime
import json

app = Flask(__name__)
LOG_FILE = "events.jsonl"


@app.route("/event", methods=["POST"])
def event():
    data = request.get_json(force=True, silent=True) or {}

    ts = datetime.now().strftime("%H:%M:%S")
    fall = data.get("fall", {})
    gas = data.get("gas", {})
    temp = data.get("temp", {})
    decision = data.get("decision", "?")

    temp_val = temp.get("celsius")
    temp_str = f"{temp_val:.1f}C" if isinstance(temp_val, (int, float)) else "--"

    line = (f"[{ts}] {fall.get('label', '?'):5s} ({fall.get('confidence', 0):.2f}) | "
            f"gas {gas.get('raw', '?')}{' ALERT' if gas.get('alert') else ''} | "
            f"temp {temp_str}{' ALERT' if temp.get('alert') else ''} | "
            f"DECISION: {decision}")

    if decision != "NORMAL":
        line = f"{line}   <<<<<<<<"
    print(line, flush=True)

    with open(LOG_FILE, "a") as f:
        f.write(json.dumps({**data, "received_at": datetime.now().isoformat()}) + "\n")

    return jsonify(ok=True)


@app.route("/", methods=["GET"])
def index():
    return "UNO Q event server running. POST events to /event", 200


if __name__ == "__main__":
    print("=" * 60)
    print("UNO Q event server on port 5000 — waiting for events...")
    print("=" * 60)
    app.run(host="0.0.0.0", port=5000)
