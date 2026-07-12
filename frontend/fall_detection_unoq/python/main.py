"""
Fall + environment monitoring — Linux/Python side (Arduino UNO Q)

- Polls the MCU (accel + gas + temp) via the Bridge
- Prints live accelerometer values
- Runs the fall detection model on a sliding window
- Applies gas/temp thresholds and makes a combined decision
- Sends everything to the AI PC's Flask server over WiFi
"""

from arduino.app_utils import *
import os
import time
import json
import urllib.request
import numpy as np
from collections import deque

# ---------------- Config ----------------
AI_PC_IP    = "10.91.54.100"   # <<< CHANGE to your AI PC's IP (ipconfig on the PC)
AI_PC_PORT  = 5000

MODEL_PATH  = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                           "fall_detection_model.pkl")
SAMPLE_HZ   = 25
WINDOW_SEC  = 2.0
PREDICT_EVERY = 0.5     # seconds between predictions / decisions / sends

CONVERT_TO_G = True     # model appears trained in g; set False if trained in m/s^2

GAS_THRESHOLD    = 400  # MQ-2 raw ADC value; calibrate by watching normal readings
TEMP_THRESHOLD_C = 45.0 # Celsius
# -----------------------------------------

AI_PC_URL = f"http://{AI_PC_IP}:{AI_PC_PORT}/event"


def load_model(path):
    try:
        import joblib
        return joblib.load(path)
    except Exception:
        import pickle
        with open(path, "rb") as f:
            return pickle.load(f)

model = load_model(MODEL_PATH)
N_FEATURES = getattr(model, "n_features_in_", None)
CLASSES = getattr(model, "classes_", None)

print("=" * 60)
print(f"Model loaded: {type(model).__name__}")
print(f"Expects {N_FEATURES} features | classes: {CLASSES}")
print(f"Sending events to {AI_PC_URL}")
print("=" * 60)

WINDOW_N = int(SAMPLE_HZ * WINDOW_SEC)
window = deque(maxlen=WINDOW_N)


def window_features(arr):
    """mean, std, min, max on x, y, z and magnitude -> 16 features."""
    mag = np.linalg.norm(arr, axis=1)
    feats = []
    for c in (arr[:, 0], arr[:, 1], arr[:, 2], mag):
        feats += [c.mean(), c.std(), c.min(), c.max()]
    return np.array(feats)


if N_FEATURES == 3:
    MODE = "per_sample"
elif N_FEATURES == 16:
    MODE = "window_stats"
elif N_FEATURES is not None and N_FEATURES % 3 == 0:
    MODE = "flat_window"
    WINDOW_N = N_FEATURES // 3
    window = deque(maxlen=WINDOW_N)
else:
    MODE = "window_stats"
print(f"Prediction mode: {MODE} (window = {WINDOW_N} samples)")

_last_pred_t = 0.0
_alert_on = False
_send_fail_count = 0


def run_model():
    arr = np.array(window)
    if MODE == "per_sample":
        X = arr[-1].reshape(1, -1)
    elif MODE == "flat_window":
        X = arr.flatten().reshape(1, -1)
    else:
        X = window_features(arr).reshape(1, -1)

    if N_FEATURES is not None and X.shape[1] != N_FEATURES:
        print(f"!! Feature mismatch: built {X.shape[1]}, model wants {N_FEATURES}.")
        return "UNKNOWN", 0.0

    label = str(model.predict(X)[0])
    conf = float(model.predict_proba(X).max()) if hasattr(model, "predict_proba") else 1.0
    return label, conf


def decide(is_fall, gas_alert, temp_alert):
    if is_fall and (gas_alert or temp_alert):
        return "CRITICAL EMERGENCY"
    if is_fall:
        return "FALL ALERT"
    if gas_alert and temp_alert:
        return "FIRE RISK"
    if gas_alert:
        return "GAS WARNING"
    if temp_alert:
        return "HEAT WARNING"
    return "NORMAL"


def send_to_pc(payload):
    global _send_fail_count
    try:
        req = urllib.request.Request(
            AI_PC_URL,
            data=json.dumps(payload).encode(),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        urllib.request.urlopen(req, timeout=1.0)
        if _send_fail_count > 0:
            print("-- AI PC connection restored --")
        _send_fail_count = 0
    except Exception as e:
        _send_fail_count += 1
        if _send_fail_count in (1, 10) or _send_fail_count % 100 == 0:
            print(f"-- Cannot reach AI PC at {AI_PC_URL}: {e} --")


def loop():
    global _last_pred_t, _alert_on
    t0 = time.time()

    try:
        raw = Bridge.call("get_data")
    except Exception as e:
        print(f"Waiting for MCU... ({e})")
        time.sleep(1.0)
        return

    try:
        parts = str(raw).split(",")
        x_ms2, y_ms2, z_ms2 = (int(v) / 1000.0 for v in parts[:3])
        gas = int(parts[3])
        temp_c = int(parts[4]) / 10.0 if int(parts[4]) != -1000 else None
    except Exception:
        print(f"Bad sample from MCU: {raw!r}")
        time.sleep(0.5)
        return

    div = 9.81 if CONVERT_TO_G else 1.0
    window.append((x_ms2 / div, y_ms2 / div, z_ms2 / div))

    if len(window) == window.maxlen and (t0 - _last_pred_t) >= PREDICT_EVERY:
        _last_pred_t = t0

        label, conf = run_model()
        is_fall = "fall" in label.lower()
        gas_alert = gas >= GAS_THRESHOLD
        temp_alert = temp_c is not None and temp_c >= TEMP_THRESHOLD_C
        decision = decide(is_fall, gas_alert, temp_alert)

        temp_str = f"{temp_c:.1f}C" if temp_c is not None else "--"
        status = (f"Accel[{x_ms2:6.2f},{y_ms2:6.2f},{z_ms2:6.2f}] m/s^2 | "
                  f"Gas: {gas}{' !' if gas_alert else ''} | "
                  f"Temp: {temp_str}{' !' if temp_alert else ''} | "
                  f"{label} ({conf:.2f}) | {decision}")
        print(status)

        # Mirror the decision on the Serial Monitor via the MCU (int code)
        codes = {"NORMAL": 0, "FALL ALERT": 1, "GAS WARNING": 2,
                 "HEAT WARNING": 3, "FIRE RISK": 4, "CRITICAL EMERGENCY": 5}
        try:
            Bridge.call("show_status", codes.get(decision, 0))
        except Exception:
            pass

        # LED on for any non-normal decision
        alert = decision != "NORMAL"
        if alert != _alert_on:
            try:
                Bridge.call("set_alert", 1 if alert else 0)
                _alert_on = alert
            except Exception:
                pass

        send_to_pc({
            "device": "uno_q",
            "timestamp": time.time(),
            "fall": {"label": label, "confidence": round(conf, 3)},
            "accel_ms2": {"x": round(x_ms2, 2), "y": round(y_ms2, 2), "z": round(z_ms2, 2)},
            "gas": {"raw": gas, "alert": gas_alert, "threshold": GAS_THRESHOLD},
            "temp": {"celsius": temp_c, "alert": temp_alert, "threshold": TEMP_THRESHOLD_C},
            "decision": decision,
        })

    elapsed = time.time() - t0
    time.sleep(max(0.0, 1.0 / SAMPLE_HZ - elapsed))


App.run(user_loop=loop)
