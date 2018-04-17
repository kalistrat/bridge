package com;

import org.eclipse.paho.client.mqttv3.*;

import javax.net.ssl.*;
import java.io.*;
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

    public MQTTService(UARTService uart)  {
        try {

            uartService = uart;
            topicProp = new Properties();
            FileInputStream input = new FileInputStream(Main.AbsPath + "topics.properties");
            topicProp.load(input);


                if (setMqttServiceProperties()) {
                    startMqttSubscriber();
                    startMqttPublisher();
                }


        } catch (Throwable e3){
            System.out.println("1 startMqttSubscriber");
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
                serverLogin = Main.prop.getProperty("MQTT_LOGIN");
                serverPassword = Main.prop.getProperty("MQTT_PASSWORD");
                mqttServerHost = Main.prop.getProperty("MQTT_HOST");
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

    public void startMqttSubscriber() {
        try{
                readClient = null;
                System.gc();

            System.out.println("mqttServerHost : " + mqttServerHost);

                readClient = new MqttClient(mqttServerHost, MqttClient.generateClientId(), null);
                readClient.connect(mqttOptions);
                readClient.setCallback(this);
                readClient.subscribe(readTopicName);

        } catch (MqttException e1) {
            //e1.printStackTrace();
            System.out.println("2 startMqttSubscriber");
            //throw  e1;
        }
    }

    public void startMqttPublisher() {
        try{

            writeClient = null;
            System.gc();
            System.out.println("writeClient start create");
            writeClient = new MqttClient(mqttServerHost, MqttClient.generateClientId(), null);
            //ystem.out.println("writeClient done");

            uartService.addUARTListener(new UARTListener() {
                @Override
                public void uartMessageSent(String message) {

//                    try{
//                        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
//                        System.out.println("try to connect to mqtt-server");
//                        writeClient.connect(mqttOptions);
//                        writeClient.publish("", mqttMessage);
//                        writeClient.disconnect();
//                    } catch (MqttException e2) {
//                        System.out.println("falied uartMessageSent in startMqttPublisher");
//                    }

                    System.out.println("MQTTService sent : " + message);

                }
            });

            System.out.println("writeClient done");

        } catch (MqttException e1) {
            System.out.println("3 startMqttSubscriber");
            //throw  e1;
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
