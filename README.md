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

# �