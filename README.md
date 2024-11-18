# Distributed System for Monitoring Sensor Readings Over Time

## Description

This project implements a **decentralized distributed system** with peer-to-peer communication between nodes using the **UDP protocol** and a **Kafka control node**. The system tracks sensor readings over time and ensures process synchronization using scalar and vector timestamps. The implementation involves the following key components:

1. **Nodes** simulating sensors that communicate with one another over a fully connected network.
2. A **Kafka coordinator node** responsible for initializing and managing the network.
3. A mechanism for **retransmitting lost UDP packets** to simulate real-world UDP communication.

---

## Architecture

The system consists of the following key components:

### 1. **Kafka Coordinator Node**
The Kafka coordinator is responsible for:
- Managing the formation of the network by sharing node identifiers.
- Sending control messages to start and stop the nodes.
- Coordinating the overall system operation through Kafka topics.

### 2. **Sensor Nodes**
Sensor nodes:
- Exchange sensor readings with all other nodes using UDP.
- Generate and synchronize sensor data using scalar and vector timestamps.
- Compute averages for the last 5 seconds of sensor readings.
- Simulate packet loss and retransmit lost packets.

### 3. **Communication Protocols**
- **Kafka Topics**: `Command` (to control nodes) and `Register` (to share node information).
- **UDP Protocol**: Used for peer-to-peer communication between nodes, including data exchange and acknowledgment of received packets.

---

## Functional Overview

### 4. Functionality of the Kafka Coordinator

#### 4.1 Starting the Node Group
- The coordinator starts the nodes by publishing a **"Start"** control message to the `Command` topic.
- Before this message is sent, all nodes must subscribe to `Command` and `Register`.

#### 4.2 Stopping the Node Group
- To stop the system, the coordinator sends a **"Stop"** control message to the `Command` topic.
- Upon sending the "Stop" message, the coordinator process terminates.

---

### 5. Functionality of the Nodes

#### 5.1 Node Initialization
- Each node initializes with a unique identifier (`id`) and a UDP port for communication.
- Nodes subscribe to Kafka topics `Command` and `Register` and await the **"Start"** message from the coordinator.

#### 5.2 Node Registration
- Upon receiving the **"Start"** message, nodes send a registration message to the `Register` topic in JSON format, containing:
  - Node ID.
  - IP address.
  - UDP port.

#### 5.3 Establishing UDP Communication
- Nodes establish communication with all other registered nodes using UDP.
- To simulate packet loss and delay, the **SimpleSimulatedDatagramSocket** class is used:
  - **Loss rate**: 30%
  - **Average delay**: 1000 ms
- If using a language other than Java, the socket's functionality must be reimplemented.

#### 5.4 Generating Sensor Readings
- Nodes emulate sensor readings using the `readings.csv` file, focusing on NO₂ concentration values.
- The row index for a reading is calculated using the formula:
  \[
  \text{row} = (\text{ActiveSeconds} \% 100) + 1
  \]
  where `ActiveSeconds` is the sensor's runtime in seconds.

#### 5.5 Exchanging Readings and Updating Timestamps
- Nodes generate and send UDP packets containing:
  - Sensor reading.
  - Updated scalar and vector timestamps.
- Upon receiving a packet, the node acknowledges it with a simpler acknowledgment packet.

#### 5.6 Retransmitting Lost Packets
- If a node does not receive an acknowledgment, it retransmits the original packet, preserving its timestamps.
- Duplicate packets are ignored but still acknowledged to ensure system stability.

#### 5.7 Sorting and Computing Averages
- Every 5 seconds, nodes:
  - Sort all readings (self-generated and received) using scalar and vector timestamps.
  - Print timestamps and sorted readings to the console.
  - Compute and display the average reading for the past 5 seconds.

#### 5.8 Stopping Nodes
- Upon receiving the **"Stop"** message:
  1. Nodes halt UDP communication.
  2. Finalize and display all readings and averages.
  3. Terminate the process.

---

## Contributors

- Lorena Martinović, University of Zagreb Faculty of Electrical Engineering and Computing


