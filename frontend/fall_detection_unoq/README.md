# Live fall detection on Arduino UNO Q

MCU (sketch) reads the ADXL345 → Bridge → Python polls samples, windows them, and runs `fall_detection_model.pkl`.

## Setup (once)

1. In App Lab, open your fall_detection app.
2. Replace `sketch/sketch.ino` with the one here, and the Python file with `python/main.py`.
3. Click **Add sketch library** (book + icon) → search **Arduino_RouterBridge** → Install.
4. Confirm the model is on the board: `ls ~/fall_detection_model.pkl`
   (if missing, re-run the scp from your laptop).
5. Packages (you likely did this already):
   `pip3 install scikit-learn numpy --break-system-packages`
   (pyserial is NOT needed anymore — the Bridge replaces it.)

## Wiring (ADXL345, I2C)

- VCC → 3.3V, GND → GND, SDA → SDA (A4), SCL → SCL (A5), CS → 3.3V, SDO → GND

## Run

Click **Run** in App Lab. Console should show:

```
Model loaded: RandomForestClassifier
Expects 16 features | classes: ['ADL' 'FALL']
Prediction mode: window_stats (window = 50 samples)
Prediction: ADL (confidence: 0.97)
```

Wait ~2 s for the window to fill before predictions start. On a fall the console prints `*** FALL DETECTED ***` and the on-board LED lights.

## If it prints a feature mismatch

The script auto-detects 3 common feature layouts. If it says
`Feature mismatch: built X, model wants N`, paste that line to Claude —
the feature extractor needs to match exactly how the model was trained.
