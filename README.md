<div align="center">

# ⚔️ MMO-AI Tactical Arena

*A next-gen, AI-driven 2D boss battle experience where the enemy learns, adapts, and counters your playstyle in real-time.*

[![LibGDX](https://img.shields.io/badge/LibGDX-1.12.0-red.svg?style=flat-square&logo=libgdx)](#)
[![Java](https://img.shields.io/badge/Java-8%2B-blue.svg?style=flat-square&logo=java)](#)
[![Python](https://img.shields.io/badge/Python-3.9%2B-yellow.svg?style=flat-square&logo=python)](#)
[![gRPC](https://img.shields.io/badge/gRPC-Enabled-brightgreen.svg?style=flat-square)](#)
[![Gemini AI](https://img.shields.io/badge/AI-Gemini-orange.svg?style=flat-square)](#)

</div>

---

## 📖 Overview

**MMO-AI Tactical Arena** is a highly dynamic 2D fighting game built on Java and LibGDX, bridged to a high-speed Python AI backend via gRPC. 

Unlike static game AI, our boss (The Necromancer) uses continuous telemetry data to rewrite its own combat script mid-fight. The game features a dynamic roster of fighters with vastly different mechanics, cinematic key-frame intros, and rich, responsive combat systems including ranged cooldowns and advanced parrying.

## ✨ Latest Updates

*   🗡️ **Sekiro Combat Logic:** Newly integrated posture and deflection mechanics for precision-based melee combat.
*   🏹 **Ironeye Ranged Class:** A fully functional long-range archer class with custom projectile tracking, 0.5s firing cooldowns, and hitbox bypassing for ranged combat.
*   👑 **King Nebuchadnezzar:** Heavy melee bruiser with unique combo strings, ultimate attacks, and a custom cinematic key-frame intro with voiceovers.
*   🎥 **Cinematic Engine:** Dynamic intro system overlaying high-fidelity static key frames with synchronized audio tracks before seamlessly transitioning into the combat state.

## 🛠️ Tech Stack

*   **Game Client:** Java, LibGDX
*   **Networking:** gRPC & Protocol Buffers (Protobuf)
*   **AI Engine:** Python, Gemini API
*   **Asset Management:** LibGDX SpriteBatch & ShapeRenderer

## 🧠 The AI Architecture

The game utilizes a **Pattern Telemetry & Adaptation Layer**:
1.  **Telemetry Stream:** The Java client sends a `GameState` packet every 0.4 seconds over gRPC (Player ID, X/Y coordinates, current action).
2.  **Rolling History:** The Python server maintains a rolling window of your combat habits (e.g., spacing, dodge frequency, projectile spam).
3.  **Dynamic Scripting:** The LLM periodically analyzes the telemetry to rewrite the boss's tactical weights on the fly (e.g., increasing gap-closing aggressiveness if the player is camping with Ironeye).
4.  **Execution:** The boss executes the newly generated combat commands in real-time.

---

## 🎮 Controls

| Action | Keybinding | Notes |
| :--- | :--- | :--- |
| **Move Left/Right** | `A` / `D` | |
| **Jump** | `W` / `SPACE` | |
| **Primary Attack** | `Left Click` | *Note: Ironeye has a 0.5s global cooldown* |
| **Secondary Attack** | `Right Click`| Disabled for ranged classes |
| **Ultimate Attack** | `E` | Unique animation per character |
| **Deflect / Parry** | `Q` | Tied to the new Sekiro logic & visual shield |

---

## 🚀 Getting Started

### Prerequisites
*   JDK 8 or higher
*   Maven
*   Python 3.9+
*   gRPC tools

### 1. Start the AI Server
```bash
cd ai-server-python
pip install grpcio grpcio-tools google-generativeai
python ai_server.py
```

### 2. Run the Game Client
```bash
cd game-server-java/gameserver
mvn compile exec:java "-Dexec.mainClass=com.mmo.GameClient"
```

---

## 🗺️ Roadmap

- [x] Dynamic Character Registry (Easy teammate additions)
- [x] Projectile lifecycle management & memory cleanup
- [x] Cinematic Intro state machine
- [x] Sekiro-style parrying logic
- [ ] Full prompt loop for LLM telemetry analysis
- [ ] Multiplayer cooperative arena (2v1 AI Boss)

---

## 🤝 Credits & Contributors

*   **Lead Development:** [Abishek Krishna (Abie-356)](https://github.com/Abie-356)
*   **Contributors:** 
    *   [Shinay Mudaliar (aizen2737)](https://github.com/aizen2737)
    *   [Sashty Madhav (sashtymadhav2007-hash)](https://github.com/sashtymadhav2007-hash)
*   **Assets:** Uses standard 2D sprite sheets. Audio/Keyframes custom-generated.
