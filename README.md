# Real‑Time IIoT Monitoring Platform

This README provides **exact, step‑by-step instructions** for Windsurf to build, test, and deploy the Real‑Time IIoT Monitoring Platform. All placeholders (enclosed in `<ANGLE_BRACKETS>`) must be left blank for the project owner to fill in.

---
## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Setup Local Development](#setup-local-development)
   - [1. Initialize Git Repository](#1-initialize-git-repository)
   - [2. Create `.env.sample`](#2-create-envsample)
   - [3. Build Simulator Module](#3-build-simulator-module)
   - [4. Build Backend Module](#4-build-backend-module)
   - [5. Build Frontend Module](#5-build-frontend-module)
   - [6. Configure Nginx](#6-configure-nginx)
   - [7. Create `docker-compose.yml`](#7-create-docker-composeyml)
5. [Local Testing](#local-testing)
6. [Deploy to AWS EC2](#deploy-to-aws-ec2)
   - [1. Clone Repo on Instance](#1-clone-repo-on-instance)
   - [2. Populate `.env`](#2-populate-env)
   - [3. Install Docker & Compose](#3-install-docker--compose)
   - [4. Start Stack](#4-start-stack)
7. [Placeholders to Fill](#placeholders-to-fill)

---
## Overview
A self‑contained full‑stack platform that:
- **Simulates** IIoT sensors publishing JSON over MQTT.
- **Ingests** data in a Java Spring Boot service and writes to InfluxDB.
- **Visualizes** in a React dashboard via Nginx reverse proxy with SSL.

## Prerequisites
- **Local machine**: Java 11+, Maven/Gradle; Node.js 16+ with npm or Yarn; Git; Docker & Docker Compose plugin.
- **AWS**: EC2 instance with Docker, Docker Compose, Nginx, Let’s Encrypt certs installed; security groups and DNS pointing `<YOUR_DOMAIN>` to the instance.

## Project Structure
```
iiot-monitoring/
├── simulator/           # MQTT sensor simulator (Java)
│   └── src/
│       └── main/java/…
├── backend/             # Spring Boot ingestor (Java)
│   └── src/
│       └── main/java/…
├── frontend/            # React dashboard (JavaScript)
│   └── src/
│       └── …
├── nginx/
│   └── conf.d/
│       └── default.conf # Nginx proxy
├── .env.sample          # environment variable template
└── docker-compose.yml   # orchestration for all services
```

## Setup Local Development

### 1. Initialize Git Repository
```bash
mkdir iiot-monitoring && cd iiot-monitoring
git init
touch README.md
```  
**Note**: Replace `README.md` content with this file.

### 2. Create `.env.sample`
At project root, create `.env.sample`:
```dotenv
INFLUXDB_ADMIN_USER=
INFLUXDB_ADMIN_PASSWORD=
MQTT_BROKER_URL=mqtt://mqtt:1883
DOMAIN=<YOUR_DOMAIN>
```  
Leave values blank except `MQTT_BROKER_URL`.

### 3. Build Simulator Module

1. `cd simulator`
2. Create a Maven project or Gradle:
   - **Maven**: `mvn archetype:generate …` then add dependency:
     ```xml
     <dependency>
       <groupId>org.eclipse.paho</groupId>
       <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
       <version>1.2.5</version>
     </dependency>
     ```
3. In `Simulator.java`, publish JSON every second:
   ```java
   // connect to <MQTT_BROKER_URL>
   // topic: factory/line1/sensorA
   // payload: { "timestamp":…, "value": randomDouble() }
   ```
4. Build and test: `mvn package` / `gradle build`.

### 4. Build Backend Module

1. `cd ../backend`
2. Initialize Spring Boot (Web, MQTT, InfluxDB client):
   ```bash
   mvn spring-boot:init …
   ```
3. In `application.yml`, reference env vars:
   ```yaml
   mqtt:
     broker-url: ${MQTT_BROKER_URL}
   influx:
     url: http://influxdb:8086
     username: ${INFLUXDB_ADMIN_USER}
     password: ${INFLUXDB_ADMIN_PASSWORD}
   ```
4. Implement an `@Service` subscribing to `factory/line1/+`, parsing JSON, writing points to InfluxDB.
5. Expose `GET /api/latest/{sensor}`.
6. Build: `mvn package`.

### 5. Build Frontend Module

1. `cd ../frontend`
2. Initialize: `npx create-react-app .`
3. Install charting: `npm install chart.js react-chartjs-2`
4. In `App.js`, fetch `/api/latest/sensorA` every 2 s and update a line chart.
5. Build: `npm run build` (for production assets).

### 6. Configure Nginx

1. Create `nginx/conf.d/default.conf`:
   ```nginx
   server {
     listen 80;
     server_name ${DOMAIN};
     return 301 https://$host$request_uri;
   }
   server {
     listen 443 ssl;
     server_name ${DOMAIN};
     ssl_certificate     /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
     ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;

     location /api/ { proxy_pass http://backend:8080/; }
     location / { proxy_pass http://frontend:3000/; }
   }
   ```
2. Leave `${DOMAIN}` placeholder unchanged.

### 7. Create `docker-compose.yml`

In project root:
```yaml
version: '3.8'
services:
  mqtt:
    image: eclipse-mosquitto
    ports: ['1883:1883']

  influxdb:
    image: influxdb:2.0
    ports: ['8086:8086']
    env_file: .env

  backend:
    build: ./backend
    ports: ['8080:8080']
    env_file: .env
    depends_on: ['mqtt','influxdb']

  frontend:
    build: ./frontend
    ports: ['3000:3000']
    depends_on: ['backend']

  nginx:
    image: nginx:stable
    ports: ['80:80','443:443']
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d
      - /etc/letsencrypt:/etc/letsencrypt
    depends_on: ['backend','frontend']
```

---
## Local Testing
```bash
cd iiot-monitoring
docker compose up -d
# Wait for all containers
docker compose logs -f
# Test API
curl http://localhost:8080/api/latest/sensorA
# Open dashboard
http://localhost:3000
```

## Deploy to AWS EC2

### 1. Clone Repo on Instance
```bash
ssh -i <PATH_TO_KEY>.pem ubuntu@<ELASTIC_IP>
cd ~
git clone <GIT_REPO_URL> iiot-monitoring
cd iiot-monitoring
```
Leave `<PATH_TO_KEY>.pem`, `<ELASTIC_IP>`, and `<GIT_REPO_URL>` blank.

### 2. Populate `.env`
```bash
cp .env.sample .env
nano .env
```
Fill in `INFLUXDB_ADMIN_PASSWORD` and `DOMAIN` only.

### 3. Install Docker & Compose
```bash
sudo apt update && sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker $USER
newgrp docker
```

### 4. Start Stack
```bash
docker compose up -d
```
Verify:
- `docker compose ps`
- `curl https://${DOMAIN}/api/latest/sensorA`
- Browse `https://${DOMAIN}`

---
## Placeholders to Fill
- `<GIT_REPO_URL>` in clone commands
- `.env`:
  - `INFLUXDB_ADMIN_PASSWORD=` (your InfluxDB admin password)
  - `DOMAIN=` (e.g., iiot.example.com)
- SSH key path `<PATH_TO_KEY>.pem` and `<ELASTIC_IP>` when SSH’ing

---
*End of README*