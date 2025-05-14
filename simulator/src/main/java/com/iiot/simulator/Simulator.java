package com.iiot.simulator;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Simulator {
    private static final String TOPIC = "factory/line1/sensorA";
    private static final String BROKER_URL = System.getenv("MQTT_BROKER_URL") != null ? 
                                            System.getenv("MQTT_BROKER_URL") : "mqtt://localhost:1883";
    private static final String CLIENT_ID = "IIoTSensorSimulator";
    private static final Random random = new Random();

    public static void main(String[] args) {
        try {
            // Setup MQTT client
            MqttClient mqttClient = new MqttClient(
                    BROKER_URL.replace("mqtt://", "tcp://"), 
                    CLIENT_ID, 
                    new MemoryPersistence()
            );
            
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setAutomaticReconnect(true);
            
            System.out.println("Connecting to MQTT broker: " + BROKER_URL);
            mqttClient.connect(connOpts);
            System.out.println("Connected to MQTT broker");
            
            // Schedule sensor data publication
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(() -> publishSensorData(mqttClient), 0, 1, TimeUnit.SECONDS);
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    executor.shutdown();
                    mqttClient.disconnect();
                    System.out.println("Disconnected from MQTT broker");
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }));
        } catch (MqttException me) {
            System.out.println("MQTT Error: " + me.getMessage());
            me.printStackTrace();
        }
    }
    
    private static void publishSensorData(MqttClient mqttClient) {
        try {
            // Generate random sensor value
            double sensorValue = generateRandomSensorValue();
            
            // Create JSON payload
            String payload = String.format(
                "{\"timestamp\":%d,\"value\":%.2f}",
                Instant.now().getEpochSecond(),
                sensorValue
            );
            
            // Publish to MQTT
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            mqttClient.publish(TOPIC, message);
            
            System.out.println("Published: " + payload);
        } catch (MqttException e) {
            System.out.println("Error publishing: " + e.getMessage());
        }
    }
    
    private static double generateRandomSensorValue() {
        // Simulate temperature between 20.0 and 30.0 degrees Celsius
        return 20.0 + (random.nextDouble() * 10.0);
    }
}
