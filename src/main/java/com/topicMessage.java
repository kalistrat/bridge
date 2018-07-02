package com;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by kalistrat on 02.07.2018.
 */
public class topicMessage {
    String topic;
    MqttMessage mqttMessage;
    String message;
    long millis;
    boolean isProceed;

    public topicMessage(String Topic, MqttMessage MQTTmessage,String Message){
        topic = Topic;
        mqttMessage = MQTTmessage;
        message = Message;
        millis = System.currentTimeMillis();
        isProceed = false;
    }
}
