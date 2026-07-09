# 03. Intelligence Hierarchy

## 3.1 Motivation

Emergency response environments are highly dynamic and often experience intermittent connectivity, infrastructure failures, and limited computational resources at the edge. Conventional rescue systems typically rely on either cloud computing or a single powerful edge device, creating a single point of failure when communication is disrupted or hardware becomes unavailable.

PHOENIX addresses this challenge through a **Hierarchical Distributed Intelligence** architecture. Instead of concentrating all decision-making on one device, intelligence is partitioned across multiple computational layers according to each device's computational capabilities, power constraints, and operational responsibilities.

Each intelligence layer contributes a unique level of reasoning while remaining capable of operating independently when higher layers become unavailable. This enables the system to **degrade gracefully**, ensuring that critical firefighter assistance continues throughout the rescue mission.

---

# 3.2 Design Philosophy

The intelligence hierarchy is built on three fundamental principles.

## 1. Role-Specific Intelligence

Every device performs AI tasks that best match its computational capabilities and mission role.

Rather than duplicating functionality across devices:

- The **Qualcomm AI Cloud** stores mission knowledge.
- The **Snapdragon X Elite AI PC** performs collaborative reasoning.
- The **Snapdragon Smartphone** understands the firefighter's immediate surroundings.
- The **Arduino UNO Q** continuously monitors firefighter safety.

This specialization reduces redundant computation while maximizing the strengths of each platform.

---

## 2. Progressive Autonomy

Operational intelligence progressively shifts closer to the firefighter whenever higher computational layers become unavailable.

Instead of depending on continuous cloud connectivity or centralized control, each layer maintains sufficient intelligence to continue operating independently.

This allows the rescue mission to continue even during communication failures.

---

## 3. Graceful Degradation

Loss of one computational layer does not terminate the mission.

Instead, the system progressively reduces available functionality while preserving essential firefighter assistance.

This hierarchical organization eliminates single points of failure and improves operational resilience.

---

# 3.3 Intelligence Hierarchy

<img width="295" height="647" alt="image" src="https://github.com/user-attachments/assets/f05e2782-47f0-41d6-b0d9-3ff63ed256e3" />


The hierarchy represents increasing proximity to the firefighter while decreasing scope of awareness.

- **Mission Intelligence** provides strategic mission knowledge.
- **Collective Intelligence** coordinates the rescue team.
- **Local Intelligence** understands the firefighter's surroundings.
- **Safety Intelligence** protects the firefighter.

---

# 3.4 Mission Intelligence

**Platform:** Qualcomm AI Cloud

### Objective

Provide mission-level knowledge before firefighters enter the rescue environment.

### Characteristics

- Highest computational resources
- Long-term mission storage
- Not latency critical
- Available before deployment
- Optional during rescue

### Responsibilities

- Store building floor plans
- Store emergency exits
- Maintain hazard databases
- Store AI models
- Maintain mission history
- Generate mission packages

### Outputs

The cloud provides the AI PC with:

- Mission package
- Building maps
- AI models
- Hazard information
- Mission configuration

### Failure Behaviour

Once deployment begins, the complete mission package has already been cached locally on the AI PC.

Loss of cloud connectivity therefore does not interrupt rescue operations.

---

# 3.5 Collective Intelligence

**Platform:** Snapdragon X Elite AI PC

### Objective

Maintain a global understanding of the rescue mission and coordinate multiple firefighters.

### Characteristics

- High-performance edge computing
- Real-time mission coordination
- Multi-firefighter reasoning
- Live digital twin generation

### Responsibilities

The AI PC continuously fuses semantic information received from every firefighter to maintain a live digital twin of the rescue operation.

Using this global mission state, it performs:

- Dynamic task allocation
- Search area assignment
- Route optimization
- Duplicate search prevention
- Hazard-aware navigation
- Rescue prioritization
- Communication quality monitoring

Unlike individual smartphones, the AI PC understands the state of the **entire rescue team**.

### Outputs

The AI PC sends:

- Updated routes
- Search assignments
- Risk maps
- Mission priorities
- Coordination instructions

### Failure Behaviour

If the AI PC becomes unavailable:

- Global coordination stops.
- Digital twin updates stop.
- Smartphones continue operating independently using onboard AI.

---

# 3.6 Local Intelligence

**Platform:** Snapdragon Smartphone

### Objective

Understand the firefighter's immediate surroundings through real-time AI inference.

### Characteristics

- Low latency
- Real-time perception
- Edge AI inference
- Independent operation

### Responsibilities

Each smartphone performs AI inference directly on camera and microphone data.

Functions include:

- Victim detection
- Hazard detection
- Audio distress detection
- Indoor navigation
- User interface
- Semantic event generation

Instead of transmitting raw sensor streams, the smartphone sends lightweight semantic information such as:

- Victim detected
- Hazard detected
- Distress sound detected
- Current location
- Device status

This significantly reduces bandwidth requirements while enabling collaborative reasoning.

### Failure Behaviour

If communication with the AI PC is lost:

- Local AI inference continues.
- Navigation continues using cached maps.
- Firefighters continue operating autonomously.

---

# 3.7 Safety Intelligence

**Platform:** Arduino UNO Q

### Objective

Continuously monitor firefighter safety using low-power sensing and lightweight inference.

### Characteristics

- Always-on operation
- Ultra-low power consumption
- Independent sensing
- Local emergency detection

### Responsibilities

The Arduino continuously monitors:

- IMU data
- Fall detection
- Inactivity detection
- Temperature
- Gas concentration
- SOS button

The Arduino performs lightweight TinyML inference and immediately reports emergencies to the firefighter's smartphone via Bluetooth Low Energy.

### Failure Behaviour

If the smartphone becomes unavailable:

- Safety sensing continues locally.
- Emergency monitoring remains active.
- Once communication is restored, alerts can be synchronized.

---

# 3.8 Scope of Intelligence

The intelligence hierarchy reflects increasing operational awareness from the firefighter upward to the mission level.

| Intelligence Layer | Scope of Awareness | Primary Responsibility |
|--------------------|--------------------|------------------------|
| Mission Intelligence | Entire mission before deployment | Mission preparation and knowledge management |
| Collective Intelligence | Entire rescue team | Team coordination and global decision-making |
| Local Intelligence | Individual firefighter's surroundings | Real-time perception and navigation |
| Safety Intelligence | Individual firefighter's physical condition | Continuous safety monitoring |

As intelligence moves down the hierarchy:

- Global awareness decreases.
- Autonomy increases.
- Dependence on communication decreases.

---

# 3.9 Graceful Intelligence Degradation

PHOENIX maintains operational continuity by progressively reducing intelligence instead of allowing catastrophic system failure.

| Failure Scenario | Remaining Intelligence | Operational Impact |
|------------------|------------------------|--------------------|
| Cloud unavailable | Collective + Local + Safety | Mission continues using cached mission package |
| AI PC unavailable | Local + Safety | Firefighters continue autonomously without global coordination |
| Smartphone unavailable | Safety | Firefighter safety monitoring remains operational |
| Arduino unavailable | Mission + Collective + Local | Mission continues without dedicated wearable safety monitoring |

This hierarchical degradation strategy ensures that no single device becomes a critical point of failure.

---

# 3.10 Key Takeaways

The PHOENIX intelligence hierarchy distributes decision-making across multiple Qualcomm devices according to their computational capabilities and operational responsibilities.

Instead of maximizing AI performance on a single platform, PHOENIX maximizes **AI availability**, allowing intelligence to progressively shift from:

**Mission Intelligence → Collective Intelligence → Local Intelligence → Safety Intelligence**

As higher computational layers become unavailable, lower layers continue providing essential functionality, enabling resilient firefighter assistance even in degraded communication environments.
