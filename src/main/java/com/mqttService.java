package com;

import org.eclipse.paho.client.mqttv3.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.net.ssl.*;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;


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

    MqttConnectOptions mqttOptions;
    List<sensorData> sensorList;

    public MQTTService()  {
        try {


            sensorList  = new ArrayList<>();
            setMqttService();


        } catch (Throwable e3){
            //e3.printStackTrace();
            System.out.println("MQTT : не удалось запустить службу");
        }

    }

    private void setSensorList(){
        try {

            sensorList.clear();
            Document xmlSensors = staticMethods.loadXMLFromString(
                    staticMethods.readFile(Main.AbsPath + "topics.xml")
            );

            Node nodeUID = (Node) XPathFactory.newInstance().newXPath()
                    .compile("/sensors").evaluate(xmlSensors, XPathConstants.NODE);


            NodeList nodeUIDList = nodeUID.getChildNodes();

            for (int i=0; i<nodeUIDList.getLength(); i++) {
                NodeList childNodeListSns = nodeUIDList.item(i).getChildNodes();

                for (int j=0; j<childNodeListSns.getLength();j++) {

                    if (childNodeListSns.item(j).getNodeName().equals("uid")) {
                        sensorList.add(new sensorData(childNodeListSns
                                .item(j).getTextContent()));
                    }
                }
            }

            for (sensorData iSns : sensorList){

                Node nodeTopic = (Node) XPathFactory.newInstance().newXPath()
                        .compile("/sensors/sensor[uid='" + iSns.UID + "']/topics").evaluate(xmlSensors, XPathConstants.NODE);

                NodeList nodeTopicList = nodeTopic.getChildNodes();



                for (int j = 0; j<nodeTopicList.getLength();j++) {
                    NodeList nodeTopicSnsList = nodeTopicList.item(j).getChildNodes();
                    String measure_type = null;
                    String topic_name = null;
                    for (int i = 0; i < nodeTopicSnsList.getLength(); i++) {

                        if (nodeTopicSnsList.item(i).getNodeName().equals("measure_type")) {
                            measure_type = nodeTopicSnsList.item(i).getTextContent();
                        }

                        if (nodeTopicSnsList.item(i).getNodeName().equals("topic_name")) {
                            topic_name = nodeTopicSnsList.item(i).getTextContent();
                        }
                    }

                    if (measure_type !=null && topic_name != null) {
                        iSns.addTopic(measure_type, topic_name);
                    }
                }

            }

//            for (sensorData iSenData : sensorList){
//                System.out.println("iSenData.UID : " + iSenData.UID);
//                for (topicData iTopicData : iSenData.TOPIC_LIST){
//                    System.out.println("iTopicData.MEASURE_TYPE : " + iTopicData.MEASURE_TYPE);
//                    System.out.println("iTopicData.TOPIC_NAME : " + iTopicData.TOPIC_NAME);
//                }
//            }




        } catch (Exception e){
            //e.printStackTrace();
            System.out.println("MQTT : Ошибка чтения файла топиков");
        }

    }

    public void setMqttService(){
        try {

            setSensorList();

            if (setMqttServiceProperties()) {
                startMqttSubscriber();
                startMqttPublisher();
            }


        } catch (Exception e3){
            System.out.println("MQTT : не удалось запустить службу");
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
            System.out.println("MQTT : setMqttServiceProperties: невозможно применить настройки");
        }
        return  mqttProprs;
    }

    public void startMqttSubscriber() {
        try{
            readClient = null;
            System.gc();

            //System.out.println("mqttServerHost : " + mqttServerHost);

            readClient = new MqttClient(mqttServerHost, MqttClient.generateClientId(), null);
            readClient.connect(mqttOptions);
            readClient.setCallback(this);
            readClient.subscribe(readTopicName);

            System.out.println("MQTT : подписчик сформировался успешно");


        } catch (MqttException e1) {
            //e1.printStackTrace();
            System.out.println("MQTT : startMqttSubscriber: центральный сервер недоступен или неверен логин и пароль");
            readClient = null;
        }
    }

    public void startMqttPublisher() {
        try{

            writeClient = null;
            System.gc();

            writeClient = new MqttClient(mqttServerHost, MqttClient.generateClientId(), null);

            MqttMessage mqttMessage = new MqttMessage("test_publisher".getBytes());

            writeClient.connect(mqttOptions);
            writeClient.publish("TEST", mqttMessage);
            writeClient.disconnect();

            System.out.println("MQTT : издатель сформировался успешно");


        } catch (MqttException e1) {
            System.out.println("MQTT : MqttPublisher : центральный сервер недоступен или неверен логин и пароль");
            writeClient = null;
        }

    }

    public void publishUIDMessage(String uid,String messAge){
//        try{
//
//            Set<Object> topics = topicProp.keySet();
//            if (topics.size()>0 && topics.contains(uid)) {
//
//                MqttMessage mqttMessage = new MqttMessage(messAge.getBytes());
//                writeClient.connect(mqttOptions);
//                writeClient.publish(topicProp.getProperty(uid), mqttMessage);
//                writeClient.disconnect();
//
//            } else {
//                System.out.println("MQTT : датчик привязан, но почему-то отсутствует в списке");
//            }
//
//        } catch (MqttException e1) {
//            System.out.println("MQTT : центральный сервер недоступен или неверен логин и пароль");
//            writeClient = null;
//        }
    }

    public void addSensorToFile(sensorData SENSOR){
        try {
            Document xmlSensors = staticMethods.loadXMLFromString(
                    staticMethods.readFile(Main.AbsPath + "topics.xml")
            );


            Element xmlSensorsElement = xmlSensors.createElement("sensor");
            Element xmlUIDElement = xmlSensors.createElement("uid");
            xmlUIDElement.setTextContent(SENSOR.UID);

            xmlSensorsElement.appendChild(xmlUIDElement);

            Element xmlTopicsElement = xmlSensors.createElement("topics");

            for (topicData iTopic : SENSOR.TOPIC_LIST) {
                Element xmlTopicElement = xmlSensors.createElement("topic");
                Element xmlMeasureTypeElement = xmlSensors.createElement("measure_type");
                xmlMeasureTypeElement.setTextContent(iTopic.MEASURE_TYPE);
                Element xmlTopicNameElement = xmlSensors.createElement("topic_name");
                xmlTopicNameElement.setTextContent(iTopic.TOPIC_NAME);
                xmlTopicElement.appendChild(xmlMeasureTypeElement);
                xmlTopicElement.appendChild(xmlTopicNameElement);
                xmlTopicsElement.appendChild(xmlTopicElement);
            }

            xmlSensorsElement.appendChild(xmlTopicsElement);
            xmlSensors.getDocumentElement().appendChild(xmlSensorsElement);

            staticMethods.loadXMLtoFile(xmlSensors);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteSensorFromFile(String removeUID){
        try {

            Document xmlSensors = staticMethods.loadXMLFromString(
                    staticMethods.readFile(Main.AbsPath + "topics.xml")
            );

            Node removeNode = (Node) XPathFactory.newInstance().newXPath()
                    .compile("/sensors/sensor[uid='" + removeUID + "']").evaluate(xmlSensors, XPathConstants.NODE);

            xmlSensors.getDocumentElement().removeChild(removeNode);

            staticMethods.loadXMLtoFile(xmlSensors);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addSensor(String UID){
        sensorList.add(new sensorData(UID));
    }

    public void addSensorTopicData(String UID,String mesType,String topicName){
        for (sensorData iSensor : sensorList){
            if (iSensor.UID.equals(UID)){
                iSensor.addTopic(mesType,topicName);
            }
        }
    }

    public sensorData getSensor(String UID){
        sensorData snsData = null;
        for (sensorData iSensor : sensorList){
            if (iSensor.UID.equals(UID)){
                snsData = iSensor;
            }
        }
        return snsData;
    }


}
