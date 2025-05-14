# Real-Time IIoT Monitoring Platform

## Live Example

You can view a live demo of this IIoT Monitoring Platform here:
[http://ec2-18-118-19-19.us-east-2.compute.amazonaws.com:3001](http://ec2-18-118-19-19.us-east-2.compute.amazonaws.com:3001)

## Purpose

This project demonstrates a complete Industrial Internet of Things (IIoT) monitoring solution that simulates real-world industrial sensor data collection, processing, storage, and visualization. It serves as both a learning platform and a starting point for actual IIoT implementations.

The system aims to replicate common IIoT scenarios where:
- Multiple sensors continuously transmit data from industrial equipment
- Data is collected via lightweight MQTT protocol (standard for IIoT applications)
- Time-series data is stored in a specialized database (InfluxDB)
- Real-time monitoring dashboards allow operators to view equipment status

## Key Features

- **Real-world Simulation**: Generates realistic sensor data patterns that mimic actual industrial equipment
- **Complete Architecture**: Demonstrates end-to-end IIoT data flow from sensors to dashboard
- **Containerized Deployment**: Easy deployment via Docker for both development and production
- **Scalable Design**: Components can be scaled to handle additional sensors and data volume
- **Real-time Visualization**: Live-updating dashboard for monitoring equipment status

---
## System Architecture

The platform consists of these core components:

- **Simulator Module**: Java application that simulates sensors publishing JSON data over MQTT
- **Backend Module**: Spring Boot service that ingests MQTT data and writes to InfluxDB
- **Frontend Module**: React dashboard that visualizes sensor data with real-time charts
- **MQTT Broker**: Message broker for reliable sensor data transmission
- **InfluxDB**: Time-series database optimized for sensor data storage
- **Nginx**: Reverse proxy to route traffic and enable secure connections

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
## Configuration and Credentials

### Setting Up Your Environment

This project uses environment variables for configuration. To get started:

1. Copy the sample environment file to create your own configuration:
   ```bash
   cp .env.sample .env
   ```

2. Edit the `.env` file and set the following values:
   ```
   INFLUXDB_ADMIN_USER=admin       # Username for InfluxDB authentication
   INFLUXDB_ADMIN_PASSWORD=yourpass # Strong password for InfluxDB
   MQTT_BROKER_URL=mqtt://mqtt:1883 # MQTT broker connection URL (default works for Docker setup)
   DOMAIN=your-server-ip-or-domain   # Your server's IP address or domain name
   ```

### For Cloud Deployment

When deploying to a cloud server such as AWS EC2:

1. Update the `DOMAIN` in your `.env` file to match your server's public IP address or domain name
2. Configure your server's security group to allow traffic on ports:
   - 80/443 (HTTP/HTTPS)
   - 1883 (MQTT)
   - 8081 (Backend API)
   - 3001 (Frontend)
   - 8086 (InfluxDB management)

3. For a production environment, consider:
   - Using stronger passwords
   - Implementing MQTT authentication
   - Setting up SSL for secure MQTT connections
   - Adding monitoring for system health

---
*End of README*