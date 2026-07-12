#  PHOENIX FIREFIGHTER ASSISTANT

Phoenix Firefighter Assistant is an **Edge AI-powered firefighting assistance system** designed to improve firefighter safety and assist in victim rescue during emergency response operations.

The system uses **three intelligent edge AI nodes** that perform real-time local inference and collaborate to provide firefighters and command centers with critical situational awareness, even in environments with poor visibility, high temperatures, smoke, and limited connectivity.

---

# 📷 ~ Application Description ~

Phoenix Firefighter Assistant consists of three cooperating edge devices:

##  1. Mobile Phone – Victim Detection

The mobile phone accompanies the firefighter throughout the rescue mission and serves as their **eyes and ears** in smoke-filled or low-visibility environments.

A locally deployed AI pipeline using **YOLOv8, YAMNet, WhisperTiming, and multimodal AI models** running on the **Qualcomm Adreno 840 GPU** performs:

- Victim detection using the camera
- Detection of cries and calls for help using the microphone
- Approximate victim distance estimation
- Real-time alerts to the firefighter

All inference is performed **entirely on-device**, ensuring low latency and operation without cloud connectivity.

---

##  2. Arduino UNO Q – Environmental & Firefighter Safety

The Arduino UNO Q continuously monitors environmental and firefighter conditions using onboard sensors.

It measures:

- Carbon monoxide (CO)
- Temperature
- Humidity
- Motion and posture using an accelerometer

A lightweight Random Forest model combined with threshold-based logic performs local safety inference to detect:

- Hazardous gas levels
- High temperatures
- Dangerous firefighter posture or falls

The processed telemetry is transmitted over Wi-Fi to the Snapdragon AI PC.

---

##  3. Snapdragon AI PC – Mission Intelligence

The Snapdragon AI PC acts as the central command node.

It collects telemetry and AI inferences from multiple firefighters to provide:

- Real-time firefighter monitoring
- Environmental hazard visualization
- AI-generated safety recommendations
- Mission timeline and event logging
- Improved situational awareness for incident commanders

---

#  Features

- Edge AI inference on all devices
- Cloud-independent operation
- Real-time victim detection
- Environmental hazard monitoring
- Firefighter posture monitoring
- AI-assisted safety recommendations
- Centralized command dashboard
- Low-latency multi-device communication

---


# 👥~ Contributors ~

| Name | Email |
|------|-------|
| **Shreya Narasimha Kamath** | shreyakamath41@gmail.com |
| **R Janaki** | janaki2005.r@gmail.com |
| **R Sameeksha** | sameeksharamesh2005@gmail.com |
| **E Abijay** | 05.abijay.e@gmail.com |
| **G R Shivaranjani** | shivaranjani.gr2023@vitstudent.ac.in |

---

# 🛠️ ~ Setup Instructions (from scratch) ~

## Prerequisites

- **Snapdragon AI PC** (Copilot+ PC) with Python 3.10+ and Git
- **Android phone** (Android 7.0+) and Android Studio (latest) on any dev machine
- **Arduino UNO Q** with [Arduino App Lab](https://docs.arduino.cc/hardware/uno-q/), plus sensors: ADXL345 accelerometer, MQ-2 gas sensor, DHT11 temperature sensor
- All devices on the **same Wi-Fi network** (tip: use the phone's hotspot — venue Wi-Fi often blocks device-to-device traffic)

```bash
git clone https://github.com/ShreyaNarasimhaKamath/phoenix-firefighter-ai.git
cd phoenix-firefighter-ai
```

## 1. Snapdragon AI PC — command node

```bash
pip install flask
python frontend/ai_pc_link/server.py
```

- Open the live dashboard at **http://localhost:5000**
- Find the PC's IP with `ipconfig` (IPv4 Address, e.g. `192.168.1.42`) — the phone and UNO Q will send events to this IP
- If prompted by Windows Firewall, click **Allow** (port 5000)

## 2. Mobile phone — victim detection app

1. Open the `frontend/` folder in Android Studio
2. Set the AI PC's IP in `PC_BASE_URL` inside
   `frontend/app/src/main/java/com/example/soundclassifier/net/EventSender.kt`
3. Build & install: **Run ▶** on a connected phone, or `./gradlew :app:assembleDebug` and install `app/build/outputs/apk/debug/app-debug.apk`
4. Grant **camera** and **microphone** permissions on first launch

> Shortcut: a pre-built debug APK is attached to every green CI run — repo **Actions** tab → latest run → artifact `soundclassifier-debug-apk`.

## 3. Arduino UNO Q — firefighter safety node

**Wiring:**

| Sensor | Connection |
|--------|------------|
| ADXL345 (I2C) | VCC→3.3V, GND→GND, SDA→A4, SCL→A5, CS→3.3V, SDO→GND |
| MQ-2 gas | Analog out → A0 |
| DHT11 temp | Data → D2 |

**App Lab setup (once):**

1. Open App Lab on the UNO Q and create/open the fall-detection app
2. Use `arduino/adxl345_test/adxl345_test/sketch/sketch.ino` as the sketch and `arduino/adxl345_test/adxl345_test/python/main.py` as the Python file
3. **Add sketch library** (book+ icon) → install **Arduino_RouterBridge**, **Adafruit ADXL345**, **Adafruit Unified Sensor**, **Adafruit BusIO**, **DHT sensor library**
4. Copy the model to the board and install Python deps:

```bash
scp arduino/adxl345_test/adxl345_test/python/fall_detection_model.pkl <user>@<unoq-ip>:~/
pip3 install scikit-learn numpy joblib --break-system-packages
```

---

# ▶️ ~ Run & Usage ~

Start in this order:

1. **AI PC**: `python frontend/ai_pc_link/server.py` → dashboard at http://localhost:5000
2. **UNO Q**: click **Run** in App Lab. Console shows `Model loaded: RandomForestClassifier` then live predictions (~2 s for the sample window to fill)
3. **Phone**: launch the SoundClassifier app

**What you'll see:**

- Phone detects emergency sounds (screams, cries for help) + people via camera, shows severity and estimated distance, and POSTs events to the PC
- UNO Q prints `*** FALL DETECTED ***` and lights the on-board LED on a fall; gas/heat warnings from MQ-2/DHT11 thresholds
- The PC dashboard shows all incoming events live (also logged to `events.jsonl`)

**Quick connectivity test:** open `http://<PC_IP>:5000` in the phone's browser — the dashboard should load. If not: same Wi-Fi? correct IP? firewall?

---

# ✅ ~ Testing ~

Every push to `main` runs the CI workflow (**Actions** tab), which must be fully green:

- **Android app** — Gradle build of the debug APK + unit tests (APK uploaded as an artifact)
- **Arduino** — all UNO Q sketches compiled for `arduino:zephyr:unoq` with the exact libraries used on-device
- **Python** — dependency install + syntax check of every script

Manual smoke test of the server without any devices:

```bash
curl -X POST http://localhost:5000/event -H "Content-Type: application/json" \
  -d '{"source":"audio","label":"Screaming","severity":"CRITICAL","score":0.9}'
```

---

# 📚 ~ References ~

- [YAMNet](https://www.tensorflow.org/hub/tutorials/yamnet) — audio event classification (TFLite)
- [EfficientDet-Lite0](https://www.tensorflow.org/lite/examples/object_detection/overview) — on-device object detection
- [Arduino UNO Q documentation](https://docs.arduino.cc/hardware/uno-q/) & [Arduino_RouterBridge](https://github.com/arduino-libraries/Arduino_RouterBridge)
- [Adafruit ADXL345](https://github.com/adafruit/Adafruit_ADXL345) and
