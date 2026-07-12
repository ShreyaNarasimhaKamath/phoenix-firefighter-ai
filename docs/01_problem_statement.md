# Problem Statement

## Background

Firefighters operating inside burning buildings face a severe loss of situational awareness due to smoke, heat, structural damage, and unreliable communication. Traditional navigation systems such as GPS become unavailable indoors, while radio communication often degrades as firefighters move deeper into the structure.


As a result, firefighters must simultaneously:
- Navigate unfamiliar buildings
- Search for victims
- Identify hazardous objects
- Maintain communication with the command center
- Monitor their own safety

Current systems typically rely on a single device or continuous connectivity. When network connectivity is lost, critical assistance systems become unavailable, forcing firefighters to rely solely on experience and manual coordination.

---

## Problem

There is currently no resilient multi-device AI system capable of maintaining operational assistance as communication infrastructure progressively fails.

Most existing solutions suffer from one or more of the following limitations:

- Dependence on cloud connectivity
- Limited offline capability
- Lack of coordination between wearable, mobile, and command systems
- Poor fault tolerance when individual devices become unavailable

---

## Proposed Solution

PHOENIX is a distributed edge AI firefighter assistance system that distributes intelligence across four computing layers:

1. Qualcomm AI Cloud
2. Snapdragon AI PC
3. Snapdragon Smartphone
4. Arduino UNO Q

Instead of relying on a single computing device, PHOENIX progressively transfers responsibilities from cloud to edge devices as connectivity degrades.

This allows firefighters to continue receiving navigation assistance, hazard awareness, victim detection, and personal safety monitoring even during partial network failure.

---

## Core Innovation

Rather than maximizing AI capability, PHOENIX maximizes AI availability.

The system demonstrates graceful degradation, where operational intelligence continues despite the loss of cloud connectivity, command-center communication, or higher-level computing resources.

---

## Objectives

The project aims to:

- Improve firefighter situational awareness
- Enable on-device AI inference for victim and hazard detection
- Support offline operation using cached mission data
- Demonstrate seamless multi-device orchestration across Qualcomm's ecosystem
- Maintain critical safety functionality under progressively degraded communication conditions
