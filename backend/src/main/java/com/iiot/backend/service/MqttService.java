package com.iiot.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiot.backend.model.SensorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MqttService {
    
    private final InfluxDbService influxDbService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, SensorData> latestSensorData = new ConcurrentHashMap<>();
    
    public MqttService(InfluxDbService influxDbService) {
        this.influxDbService = influxDbService;
    }
    
    public void processMqttMessage(String topic, String payload) {
        try {
            log.info("Received MQTT message from topic: {}, payload: {}", topic, payload);
            
            // Extract the sensor name from the topic path
            String sensorName = topic.substring(topic.lastIndexOf("/") + 1);
            
            // Parse JSON
            JsonNode jsonNode = objectMapper.readTree(payload);
            long timestamp = jsonNode.get("timestamp").asLong();
            double value = jsonNode.get("value").asDouble();
            
            // Create sensor data object
            SensorData sensorData = new SensorData(
                    sensorName,
                    value,
                    Instant.ofEpochSecond(timestamp)
            );
            
            // Store latest data in memory
            latestSensorData.put(sensorName, sensorData);
            
            // Write to InfluxDB
            influxDbService.writeSensorData(sensorData);
            
        } catch (Exception e) {
            log.error("Error processing MQTT message: {}", e.getMessage(), e);
        }
    }
    
    public SensorData getLatestSensorData(String sensorName) {
        return latestSensorData.get(sensorName);
    }
    
    public Map<String, SensorData> getAllLatestSensorData() {
        return new HashMap<>(latestSensorData);
    }
}
