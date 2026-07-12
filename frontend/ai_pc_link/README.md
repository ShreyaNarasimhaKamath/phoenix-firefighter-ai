# Sending detections to the AI PC

Both apps POST JSON events over Wi-Fi to a Flask server on the AI PC.

```
Sound Classifier (phone) ──┐
                           ├── HTTP POST /event ──> AI PC (server.py :5000) ──> live dashboard
Object Detection (phone) ──┘
```

## 1. AI PC setup

```
pip install flask
python server.py
```

Find the PC's IP: `ipconfig` → IPv4 Address (e.g. `192.168.1.42`).
Dashboard: open http://localhost:5000
If phones can't reach it, allow Python/port 5000 through Windows Firewall
(or run once and click "Allow" on the firewall popup).

## 2. Sound Classifier app (already wired)

Just edit the IP in
`app/src/main/java/com/example/soundclassifier/net/EventSender.kt`
(`PC_BASE_URL`) and rebuild. Emergency events (label, severity, confidence,
proximity) are sent automatically.

## 3. Object Detection app (drop-in patch)

Copy from `object_detection_patch/` into your object_detection project
(`object_detection/android/`):

1. `EventSender.kt` → `app/src/main/java/org/tensorflow/lite/examples/objectdetection/EventSender.kt`
   (edit `PC_BASE_URL` to the PC's IP)
2. `CameraFragment.kt` → replace `app/src/main/java/org/tensorflow/lite/examples/objectdetection/fragments/CameraFragment.kt`
3. `AndroidManifest.xml` → replace `app/src/main/AndroidManifest.xml`

Rebuild. Detections (labels, scores, boxes) are sent max once per second.

## Event format

Audio:  `{"source":"audio","label":"Screaming","severity":"CRITICAL","score":0.78,"proximity":"Nearby","proximity_range":"2–5 m","rms_db":-18.2,"timestamp":...}`
Vision: `{"source":"vision","label":"person, dog","objects":[{"label":"person","score":0.91,"box":{...}}],"timestamp":...}`

## Troubleshooting

- Nothing arrives → phone and PC on the same Wi-Fi? IP correct? Firewall open?
  Test from the phone browser: `http://<PC_IP>:5000` should show the dashboard.
- Hotspot tip: run the phone's hotspot and connect the PC to it (hotspot
  networks don't block device-to-device traffic like some venue Wi-Fi does).
- Check Logcat tag `EventSender` for send errors.
