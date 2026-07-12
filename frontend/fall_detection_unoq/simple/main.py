"""
Firefighter monitor — Linux/Python side (Arduino UNO Q)
Simple if/else logic (no ML model needed):
  - Fall: impact spike or free-fall dip in acceleration magnitude
  - Gas / temp: thresholds
Sends everything to the AI PC over WiFi.
"""

from arduino.app_utils import *
import time
import json
import math
import urllib.request

# ---------------- Config ----------------
AI_PC_URL = "http://10.91.62.10:5000/event"

FALL_IMPACT_MS2   = 20.0   # magnitude above this = impact -> FELL DOWN
FALL_FREEFALL_MS2 = 3.0    # magnitude below this = free fall -> FELL DOWN
FALL_HOLD_SEC     = 5.0    # keep FELL DOWN status this long after detection

GAS_THRESHOLD     = 400    # MQ-2 raw ADC
TEMP_THRESHOLD_C  = 45.0   # Celsius

SEND_EVERY        = 0.5    # seconds between sends to the AI PC
# -----------------------------------------

fall_until = 0.0
_last_send = 0.0
_alert_on = False
_fails = 0

print("=" * 55)
print(f"Firefighter monitor running. Sending to {AI_PC_URL}")
print("=" * 55)


def send_to_pc(payload):
    global _fails
    try:
        req = urllib.request.Request(
            AI_PC_URL,
            data=json.dumps(payload).encode(),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        urllib.request.urlopen(req, timeout=1.0)
        if _fails > 0:
            print("-- AI PC connection restored --")
        _fails = 0
    except Exception as e:
        _fails += 1
        if _fails in (1, 10) or _fails % 100 == 0:
            print(f"-- Cannot reach AI PC: {e} --")


def loop():
    global fall_until, _last_send, _alert_on
    now = time.time()

    # --- read sensors from MCU ---
    try:
        raw = Bridge.call("get_data")
        parts = str(raw).split(",")
        x = int(parts[0]) / 1000.0
        y = int(parts[1]) / 1000.0
        z = int(parts[2]) / 1000.0
        gas = int(parts[3])
        temp_c = int(parts[4]) / 10.0 if int(parts[4]) != -1000 else None
    except Exception as e:
        print(f"Waiting for MCU... ({e})")
        time.sleep(1.0)
        return

    # --- fall detection: simple if/else on acceleration magnitude ---
    mag = math.sqrt(x * x + y * y + z * z)
    if mag > FALL_IMPACT_MS2 or mag < FALL_FREEFALL_MS2:
        fall_until = now + FALL_HOLD_SEC

    fell_down = now < fall_until
    posture = "FELL DOWN" if fell_down else "STANDING"

    # --- gas / temp thresholds ---
    gas_alert = gas >= GAS_THRESHOLD
    temp_alert = temp_c is not None and temp_c >= TEMP_THRESHOLD_C

    # --- combined decision ---
    if fell_down and (gas_alert or temp_alert):
        decision = "CRITICAL EMERGENCY"
    elif fell_down:
        decision = "FALL ALERT"
    elif gas_alert and temp_alert:
        decision = "FIRE RISK"
    elif gas_alert:
        decision = "GAS WARNING"
    elif temp_alert:
        decision = "HEAT WARNING"
    else:
        decision = "NORMAL"

    # --- LED on board for any alert ---
    alert = decision != "NORMAL"
    if alert != _alert_on:
        try:
            Bridge.call("set_alert", 1 if alert else 0)
            _alert_on = alert
        except Exception:
            pass

    # --- print + send at SEND_EVERY interval ---
    if now - _last_send >= SEND_EVERY:
        _last_send = now
        temp_str = f"{temp_c:.1f}C" if temp_c is not None else "--"
        print(f"Accel[{x:6.2f},{y:6.2f},{z:6.2f}] mag={mag:5.2f} | "
              f"Gas: {gas}{' !' if gas_alert else ''} | "
              f"Temp: {temp_str}{' !' if temp_alert else ''} | "
              f"{posture} | {decision}")

        send_to_pc({
            "device": "uno_q_firefighter",
            "timestamp": now,
            "posture": posture,
            "accel": {"x": round(x, 2), "y": round(y, 2), "z": round(z, 2),
                      "magnitude": round(mag, 2)},
            "gas": {"raw": gas, "alert": gas_alert},
            "temp": {"celsius": temp_c, "alert": temp_alert},
            "decision": decision,
        })

    time.sleep(0.05)  # ~20 samples/sec so impacts aren't missed


App.run(user_loop=loop)
