package com;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.eclipse.paho.client.mqttv3.*;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Created by kalistrat on 13.04.2018.
 */
public class MQTTService implements MqttCallback {

    MqttClient readClient;
    MqttClient writeClient;
    String serverLogin;
    String serverPassword;
    String mqttServerHost;
    String readTopicName;
    String mqttUID;
    UARTService uartService;
    MqttConnectOptions mqttOptions;
    Properties topicProp;

    public MQTTService(UARTService uart) throws Throwable {

        uartService = uart;
        topicProp = new Properties();
        FileInputStream input = new FileInputStream(Main.AbsPath + "topics.properties");
        topicProp.load(input);

        try{

            if (setMqttServiceProperties()){
                startMqttSubscriber();
                startMqttPublisher();
            }


        } catch (MqttException e1) {
            //log
            throw  e1;
        }

    }

    public SSLSocketFactory configureSSLSocketFactory() throws KeyManagementException, NoSuchAlgorithmException,
            UnrecoverableKeyException, IOException, CertificateException, KeyStoreException {
        KeyStore ks = KeyStore.getInstance("JKS");
        String jksLocation = Main.AbsPath + Main.prop.getProperty("MQTT_KEY_NAME");
        InputStream jksInputStream = new FileInputStream(jksLocation);
        ks.load(jksInputStream, "3Point".toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "3Point".toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext sc = SSLContext.getInstance("TLS");
        TrustManager[] trustManagers = tmf.getTrustManagers();
        sc.init(kmf.getKeyManagers(), trustManagers, null);

        SSLSocketFactory ssf = sc.getSocketFactory();
        return ssf;
    }

    public void connectionLost(Throwable cause) {
        // TODO Auto-generated method stub

    }


    public void messageArrived(String topic, MqttMessage message) {
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void deliveryComplete(IMqttDeliveryToken token) {
        // TODO Auto-generated method stub
    }

    public boolean setMqttServiceProperties(){
        boolean mqttProprs = false;
        try {

            if (
                    (Main.prop.getProperty("MQTT_HOST") != null)
                    && (Main.prop.getProperty("MQTT_PASSWORD") != null)
                    && (Main.prop.getProperty("MQTT_LOGIN") != null)
                    && (Main.prop.getProperty("MQTT_FROM_SERVER_TOPIC") != null)
                    && (Main.prop.getProperty("MQTT_UID") != null)

                    ) {
                serverLogin = Main.prop.getProperty("MQTT_HOST");
                serverPassword = Main.prop.getProperty("MQTT_PASSWORD");
                mqttServerHost = Main.prop.getProperty("MQTT_LOGIN");
                readTopicName = Main.prop.getProperty("MQTT_FROM_SERVER_TOPIC");
                mqttUID = Main.prop.getProperty("MQTT_UID");


                mqttOptions = new MqttConnectOptions();
                mqttOptions.setUserName(serverLogin);
                mqttOptions.setPassword(serverPassword.toCharArray());

                if (mqttServerHost.contains("ssl://")) {
                    SSLSocketFactory ssf = configureSSLSocketFactory();
                    mqttOptions.setSocketFactory(ssf);
                }
                mqttOptions.setConnectionTimeout(0);

                mqttProprs = true;
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        return  mqttProprs;
    }

    public void startMqttSubscriber() throws Throwable {
        try{
                readClient = null;
                System.gc();

                readClient = new MqttClient(mqttServerHost, MqttClient.generateClientId(), null);
                readClient.connect(mqttOptions);
                readClient.setCallback(this);
                readClient.subscribe(readTopicName);

        } catch (MqttException e1) {
            //log
            throw  e1;
        }
    }

    public void startMqttPublisher() throws Throwable {
        try{

            writeClient = null;
            System.gc();

            writeClient = new MqttClient(mqttServerHost, MqttClient.generateClientId(), null);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(serverLogin);
            options.setPassword(serverPassword.toCharArray());

            if (mqttServerHost.contains("ssl://")) {
                SSLSocketFactory ssf = configureSSLSocketFactory();
                options.setSocketFactory(ssf);
            }

            uartService.addUARTListener(new UARTListener() {
                @Override
                public void uartMessageReceive(String message) {

                    try{
                        MqttMessage mqttMessage = new MqttMessage(message.getBytes());

                        writeClient.connect(mqttOptions);
                        writeClient.publish("", mqttMessage);
                        writeClient.disconnect();
                    } catch (MqttException e2) {
                        //log
                    }

                }
            });

        } catch (MqttException e1) {
            //log
            throw  e1;
        }

    }

    private void addInTopicList(String uid,String topic){
        try {
            FileOutputStream output = new FileOutputStream(Main.AbsPath + "topics.properties");
            topicProp.put(uid, topic);
            topicProp.store(output, null);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void deleteFromTopicList(){

    }

}
