package com.iiot.backend.controller;

import com.iiot.backend.model.SensorData;
import com.iiot.backend.service.MqttService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SensorController {

    private final MqttService mqttService;

    public SensorController(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @GetMapping("/latest/{sensor}")
    public ResponseEntity<SensorData> getLatestSensorData(@PathVariable String sensor) {
        SensorData sensorData = mqttService.getLatestSensorData(sensor);
        if (sensorData == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sensorData);
    }

    @GetMapping("/latest")
    public ResponseEntity<Map<String, SensorData>> getAllLatestSensorData() {
        Map<String, SensorData> allData = mqttService.getAllLatestSensorData();
        return ResponseEntity.ok(allData);
    }
}
