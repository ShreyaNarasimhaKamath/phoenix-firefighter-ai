# System Overview

## PHOENIX: Hierarchical Distributed Edge AI for Firefighter Assistance

PHOENIX is a resilient multi-device edge AI system designed to assist firefighters operating in hazardous indoor environments where visibility, communication, and infrastructure may progressively fail.

Unlike conventional systems that rely on a single device or continuous cloud connectivity, PHOENIX distributes intelligence across four computational layers. Each layer is responsible for a different level of decision-making, ensuring that critical assistance continues even as higher-level infrastructure becomes unavailable.

The architecture consists of four intelligence layers:

1. **Mission Intelligence (Qualcomm AI Cloud)**
2. **Collective Intelligence (Snapdragon X Elite AI PC)**
3. **Local Intelligence (Snapdragon Smartphone)**
4. **Safety Intelligence (Arduino UNO Q)**

Rather than duplicating functionality across devices, every layer contributes a unique capability to the rescue mission.

---

## Mission Preparation

Before firefighters enter the building, the Qualcomm AI Cloud stores:

- Building floor plans
- Emergency exits
- Hazard information
- Mission configuration
- AI models

The Snapdragon X Elite AI PC downloads and caches the complete mission package locally before deployment. This allows the rescue mission to continue even if cloud connectivity is lost during operation.

---

## Local Intelligence (Snapdragon Smartphone)

Each firefighter carries a Snapdragon smartphone mounted on the helmet or chest.

The smartphone serves as the primary edge AI device and performs:

- Real-time victim detection
- Hazard detection
- Audio distress detection
- Indoor navigation
- User interface
- Communication with the AI PC

Instead of transmitting continuous camera or microphone streams, the phone performs AI inference locally and sends only lightweight semantic information such as:

- Victim detected
- Hazard detected
- Distress sound detected
- Current location
- Device status

This minimizes bandwidth while enabling real-time collaboration.

---

## Safety Intelligence (Arduino UNO Q)

Each firefighter also carries an Arduino UNO Q connected to safety sensors.

The Arduino continuously monitors:

- IMU data
- Fall detection
- Inactivity detection
- Environmental sensors (temperature, gas)
- Emergency SOS button

The Arduino performs lightweight always-on inference for firefighter safety and communicates alerts to the smartphone using Bluetooth Low Energy.

Because of its extremely low power consumption, this layer remains operational even if higher-level computing resources become unavailable.

---

## Collective Intelligence (Snapdragon X Elite AI PC)

The Snapdragon AI PC acts as the mission intelligence engine.

Instead of simply displaying information, it continuously builds a live digital twin of the rescue operation by combining information received from every firefighter.

The digital twin contains:

- Firefighter locations
- Victim detections
- Hazard detections
- Environmental conditions
- Search progress
- Connectivity status
- Firefighter health status

Using this global view, the AI PC performs collaborative reasoning across the entire rescue team.

Examples include:

- Assigning firefighters to unexplored areas
- Avoiding duplicate searches
- Re-routing firefighters around hazards
- Prioritizing rescue operations
- Monitoring communication quality
- Maintaining overall mission awareness

Unlike the smartphone, which only understands one firefighter's surroundings, the AI PC understands the state of the complete rescue mission.

---

## Mission Intelligence (Qualcomm AI Cloud)

The Qualcomm AI Cloud provides mission-level knowledge before deployment.

It stores:

- Building information
- Mission history
- AI models
- Hazard databases

During normal operation it synchronizes with the AI PC whenever connectivity is available.

Cloud connectivity is optional during rescue operations because the complete mission package is cached locally before deployment.

---

## Hierarchical Adaptive Intelligence

The key innovation of PHOENIX is Hierarchical Adaptive Intelligence.

Instead of relying on a single computing device, intelligence progressively shifts between computational layers as infrastructure becomes unavailable.

### Level 1 — Cloud Failure

If cloud connectivity is lost:

- Mission continues using cached data.
- No operational functionality is interrupted.

### Level 2 — AI PC Failure

If the AI PC becomes unavailable:

- Global coordination is lost.
- Each firefighter continues operating autonomously using local AI running on the smartphone.

### Level 3 — Smartphone Failure

If the smartphone becomes unavailable:

- The Arduino UNO Q continues monitoring firefighter safety.
- Fall detection and emergency alerts remain operational.

### Level 4 — Firefighter Sensor Loss

If a firefighter's sensing system is completely unavailable:

- The AI PC detects the missing node.
- Nearby firefighters are automatically reassigned to continue the search mission.

---

## Core Innovation

PHOENIX introduces a hierarchical distributed AI architecture in which every computing layer owns a different level of operational intelligence:

- Cloud → Mission Intelligence
- AI PC → Collective Intelligence
- Smartphone → Local Intelligence
- Arduino → Safety Intelligence

Rather than maximizing AI performance on a single device, PHOENIX maximizes operational resilience by ensuring that critical AI assistance remains available despite progressive loss of connectivity or computing resources.


<img width="547" height="586" alt="image" src="https://github.com/user-attachments/assets/1b395cbe-c12c-46ac-a4c3-f0be5c609402" />

