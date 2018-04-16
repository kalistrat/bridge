package com;

import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Created by kalistrat on 16.04.2018.
 */
public interface UARTListenable {
    void addUARTListener(UARTListener listener);
}
