# 🚀 **System Flow Investigator**

A real-time observability and debugging tool for distributed systems.

The **System Flow Investigator** enables engineers to trace message flows across services by capturing and visualizing data from multiple communication protocols such as **MQTT** and **WebSocket**.

---

# 🎯 **Goal**

When systems behave incorrectly (e.g. UI stops updating, messages disappear), this tool helps answer:

- ❓ Where did the flow stop?
- 🔍 Which service or channel is failing?
- 📡 Are messages reaching the UI layer?
- ⚡ What is happening in real time across the system?

---

# 🧠 **Core Concept**

All incoming messages — regardless of protocol — are normalized into a unified model:

```json
{
  "protocol": "MQTT | WS",
  "source": "host:port | connectionName",
  "channel": "topic or logical channel",
  "timestamp": "...",
  "payload": "...",
  "metadata": {},
  "traceId": "optional"
}
```

This allows consistent processing, filtering, and visualization.

---

# 🏗️ **Architecture Overview**

```text
External Systems
    │
    ├── MQTT Broker (EMQX)
    └── WebSocket Servers
            │
            ▼
   ┌──────────────────────────────┐
   │   System Flow Investigator   │
   │                              │
   │  🔌 Ingestion Layer          │
   │   - MQTT Observer            │
   │   - WebSocket Observer       │
   │                              │
   │  ⚙️ Event Pipeline            │
   │   - ObservedEventPipeline    │
   │   - RecentEventStore         │
   │   - EventHub (SSE)           │
   │   - File Sink (optional)     │
   │                              │
   │  🌐 API Layer                │
   │   - Control APIs             │
   │   - Query APIs               │
   │   - Streaming APIs           │
   └──────────────┬───────────────┘
                  │
                  ▼
               🌐 Web UI
```
---
# 📦 **Project Structure**

```text
system-flow-investigator/
├── api/              # REST controllers
├── domain/           # DTOs (ObservedEvent, requests)
├── ingestion/        # All ingestion sources
│   ├── mqtt/
│   ├── websocket/
│   ├── IngestionSource.java
│   ├── AbstractIngestionSource.java
│   └── ObservedEventPipeline.java
├── storage/          # Event storage
├── stream/           # SSE streaming
├── service/          # Facade + orchestration
```
---

## 🔌 **Supported Protocols** 

### MQTT ###
 - Connects to EMQX broker
 - Supports topic filters (lab/flow/#)
 - Handles high-throughput event streams 
### WebSocket ###
Connects as a client to external WS servers
Receives live UI messages
Maps messages into logical channels
---

## ⚙️ Core Components
### 1. IngestionSource (Generic Interface)
   ```IngestionSource<C, S>```

Defines:
 - connect
 - subscribe
 - observedChannels

Implemented by:
- MQTT observer
- WebSocket observer
---
### 2. ObservedEventPipeline

Central processing pipeline:

Event → Store → Stream → Persist

Handles:
- in-memory storage
- live streaming (SSE)
- optional file persistence

---

### 3. InvestigatorFacade

Acts as the orchestration layer:

connects ingestion sources
exposes APIs
aggregates data for UI

---

## 🌐 API Reference
### Control APIs
```http
POST /api/control/mqtt/connect
POST /api/control/mqtt/subscribe

POST /api/control/ws/connect
POST /api/control/ws/subscribe
```
### Query APIs
```http
GET /api/events/recent
GET /api/events/recent?channel=lab/flow/in
GET /api/events/recent?channel=lab/flow/in&channel=lab/flow/out

GET /api/events/mqtt/topics
GET /api/events/ws/channels
```

### Streaming API (SSE)
```http
GET /api/stream/events
GET /api/stream/events?channel=lab/flow/in
```

### 🧪 Flow Lab (Test Environment)

Included test services:

1. flow-lab-producer
   publishes to: lab/flow/in
2. flow-lab-consumer-producer
   consumes: lab/flow/in
   publishes: lab/flow/out
3. flow-lab-consumer
   consumes: lab/flow/out
   exposes WebSocket:
   ws://localhost:8090/ws/live
   ▶️ Running the System
   docker compose up --build

Open: http://localhost:8080

---

## 📊 Features by Phase
### ✅ Phase 1 — Core Ingestion
- MQTT ingestion
- WebSocket ingestion
- Unified event model
- Event pipeline
- Recent events API
- SSE live streaming

#### ✅ Phase 2 — Exploration & Filtering

- Multi-channel filtering
- Text filtering
- TraceId filtering
- UI dashboard
- Start / Stop streaming
- Multi-client support
- Channel discovery (MQTT + WS)

#### 🚧 Phase 3 — Correlation (Next)
##### Goal: Understand flows across services

##### Planned:
- TraceId propagation
- Group events by traceId
- Flow reconstruction
- Timeline visualization

---

## 🏁 Summary

### You now have a system that provides:

#### 🔥 Real-time observability 
#### 🔄 Unified ingestion (MQTT + WebSocket)
#### 📊 Live UI with filtering
#### 🧱 Clean, extensible architecture
#### 🚀 Foundation for advanced debugging tools