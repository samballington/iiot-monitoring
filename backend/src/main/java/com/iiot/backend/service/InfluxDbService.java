package com.iiot.backend.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.iiot.backend.model.SensorData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class InfluxDbService {
    private static final Logger log = LoggerFactory.getLogger(InfluxDbService.class);

    @Value("${influx.url}")
    private String url;

    @Value("${influx.org}")
    private String org;

    @Value("${influx.bucket}")
    private String bucket;

    @Value("${influx.username}")
    private String username;

    @Value("${influx.password}")
    private String password;

    private InfluxDBClient influxDBClient;
    private WriteApi writeApi;

    @PostConstruct
    public void init() {
        log.info("Initializing InfluxDB client with URL: {}", url);
        
        try {
            // Create connection options with all required parameters
            com.influxdb.client.InfluxDBClientOptions options = com.influxdb.client.InfluxDBClientOptions.builder()
                .url(url)
                .authenticateToken(String.format("%s:%s", username, password).toCharArray())
                .org(org)
                .bucket(bucket)
                .build();
            
            // Create client with the options
            influxDBClient = InfluxDBClientFactory.create(options);
            
            // Get non-blocking write client
            writeApi = influxDBClient.getWriteApi();
            
            log.info("InfluxDB client initialized successfully with org: {}, bucket: {}", org, bucket);
        } catch (Exception e) {
            log.error("Failed to initialize InfluxDB client: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void close() {
        if (writeApi != null) {
            writeApi.close();
        }
        if (influxDBClient != null) {
            influxDBClient.close();
        }
    }

    public void writeSensorData(SensorData sensorData) {
        try {
            // Create a point with measurement, tag, field and timestamp
            Point point = Point.measurement("sensor_readings")
                    .addTag("sensor", sensorData.getSensorName())
                    .addField("value", sensorData.getValue())
                    .time(sensorData.getTimestamp(), WritePrecision.S);

            // Write point to InfluxDB
            writeApi.writePoint(point);
            log.debug("Successfully wrote point to InfluxDB: {}", point);
        } catch (Exception e) {
            log.error("Error writing to InfluxDB: {}", e.getMessage(), e);
        }
    }
}
