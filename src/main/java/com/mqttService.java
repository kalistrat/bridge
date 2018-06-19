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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.Certificate;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Base64;
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
    String writeTopicName;
    String mqttUID;

    MqttConnectOptions mqttOptions;
    List<sensorData> sensorList;
    HTTPService httpService;

    public MQTTService(HTTPService hService)  {
        try {


            sensorList  = new ArrayList<>();
            httpService = hService;
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
            createClientKeyStore();

            if (setMqttServiceProperties()) {
                startMqttSubscriber();
                startMqttPublisher();
            }


        } catch (Exception e3){
            System.out.println("MQTT : setMqttService : не удалось запустить службу");
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

            String predLogin = Main.prop.getProperty("MQTT_LOGIN");
            String predPassword = Main.prop.getProperty("MQTT_PASSWORD");
            String predServerHost = Main.prop.getProperty("MQTT_HOST");
            String predReadTopicName = Main.prop.getProperty("MQTT_FROM_SERVER_TOPIC");
            String predWriteTopicName = Main.prop.getProperty("MQTT_TO_SERVER_TOPIC");
            String predMqttUID = Main.prop.getProperty("MQTT_UID");

            if (
                    (!predLogin.equals(""))
                    && (!predPassword.equals(""))
                    && (!predServerHost.equals(""))
                    && (!predReadTopicName.equals(""))
                    && (!predWriteTopicName.equals(""))
                    && (!predMqttUID.equals(""))

                    ) {
                serverLogin = Main.prop.getProperty("MQTT_LOGIN");
                serverPassword = Main.prop.getProperty("MQTT_PASSWORD");
                mqttServerHost = Main.prop.getProperty("MQTT_HOST");
                readTopicName = Main.prop.getProperty("MQTT_FROM_SERVER_TOPIC");
                writeTopicName = Main.prop.getProperty("MQTT_TO_SERVER_TOPIC");
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
                System.out.println("MQTT : setMqttServiceProperties: настройки успешно применены");

            } else {
                if (Main.prop.getProperty("MQTT_UID") != null) {
                    String linkResponse = httpService.linkDevice(Main.prop.getProperty("MQTT_UID"));

                    if (!linkResponse.contains("ERROR")){

                        serverLogin = staticMethods.getResponseAttrValue("mqttLogin",linkResponse);
                        serverPassword = staticMethods.getResponseAttrValue("mqttPassword",linkResponse);
                        mqttServerHost = staticMethods.getResponseAttrValue("mqttHost",linkResponse);
                        readTopicName = staticMethods.getResponseAttrValue("fromServerTopic",linkResponse);
                        writeTopicName = staticMethods.getResponseAttrValue("toServerTopic",linkResponse);
                        setCAFile(staticMethods.getResponseAttrValue("certBase64",linkResponse));
                        importCAtoKeyStore();
                        mqttUID = Main.prop.getProperty("MQTT_UID");


                        Main.prop.setProperty("MQTT_LOGIN",serverLogin);
                        Main.prop.setProperty("MQTT_PASSWORD",serverPassword);
                        Main.prop.setProperty("MQTT_HOST",mqttServerHost);
                        Main.prop.setProperty("MQTT_FROM_SERVER_TOPIC",readTopicName);
                        Main.prop.setProperty("MQTT_TO_SERVER_TOPIC",writeTopicName);

                        FileOutputStream output = new FileOutputStream(Main.AbsPath + "config.properties");
                        Main.prop.store(output,"bridge linked");


                    } else {
                        System.out.println("MQTT : setMqttServiceProperties: привязка бриджа завершилась с ошибкой : " + linkResponse);
                    }
                } else {
                    System.out.println("MQTT : setMqttServiceProperties: UID бриджа не задан. Его привязка невозможна");
                }

            }

        } catch (Exception e){
            e.printStackTrace();
            System.out.println("MQTT : setMqttServiceProperties: невозможно применить настройки");
        }
        return  mqttProprs;
    }

    public void startMqttSubscriber() {
        try{
            readClient = null;
            System.gc();

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
            e1.printStackTrace();
            System.out.println("MQTT : MqttPublisher : центральный сервер недоступен или неверен логин и пароль");
            writeClient = null;
        }

    }

    public void publishUIDMessage(String uid,List<String> messAge){
        try{
            //UID:TIME(unix):VALUES:STATE
            //SNS-123123:42141231341:34.3:5
            String measureValues = messAge.get(2).replace(" ","");
            String unixTime = messAge.get(1).replace(" ","");
            String stateValue = messAge.get(3).replace(" ","");

            List<String> messValPieces = staticMethods.getListFromString(measureValues,";");

            String val_1 = messValPieces.get(0).replace(" ","");

            MqttMessage mqttMessage = new MqttMessage((unixTime + ":" +val_1).getBytes());
            writeClient.connect(mqttOptions);
            writeClient.publish(getSensor(uid).TOPIC_LIST.get(0).TOPIC_NAME, mqttMessage);
            writeClient.disconnect();

            System.out.println("MQTT : сообщение датчика с " + uid + " - успешно отправлено");

        } catch (MqttException e1) {
            e1.printStackTrace();
            System.out.println("MQTT : центральный сервер недоступен или неверен логин и пароль");
            writeClient = null;
        }
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

    public sensorData addSensor(String UID){
        sensorData sns = new sensorData(UID);
        sensorList.add(sns);
        return sns;
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

    public boolean isSensorLinked(String UID){
        boolean linked = false;
        for (sensorData iSns : sensorList){
            if (iSns.UID.equals(UID)){
                linked = true;
            }
        }
        return linked;
    }

    private void createClientKeyStore(){

        try {
            File file = new File(Main.AbsPath + "/clientkeystore.jks");
            KeyStore keyStore = KeyStore.getInstance("JKS");
            if (file.exists()) {
                // if exists, load
                keyStore.load(new FileInputStream(file), "3Point".toCharArray());
            } else {
                // if not exists, create
                keyStore.load(null, null);
                keyStore.store(new FileOutputStream(file), "3Point".toCharArray());
            }
        } catch (Exception e){
            e.printStackTrace();
        }

    }
    private void setCAFile(String base64crt){
        try {

            byte[] decoded = Base64.getDecoder().decode(base64crt);

            try (FileOutputStream fos = new FileOutputStream(Main.AbsPath+"/client_dec.crt")) {
                fos.write(decoded);
                fos.close();
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    private void importCAtoKeyStore(){
        try {

            FileInputStream is = new FileInputStream(Main.AbsPath+ "/clientkeystore.jks");

            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, "3Point".toCharArray());

            String alias = "testserver";
            char[] password = "3Point".toCharArray();

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream certstream = fullStream (Main.AbsPath+ "/client_dec.crt");
            //Certificate certs = cf.generateCertificate(certstream);
            java.security.cert.Certificate certs = cf.generateCertificate(certstream);


            File keystoreFile = new File(Main.AbsPath+ "/clientkeystore.jks");

            // Load the keystore contents
            FileInputStream in = new FileInputStream(keystoreFile);
            keystore.load(in, password);
            in.close();

            // Add the certificate
            keystore.setCertificateEntry(alias, certs);

            // Save the new keystore contents
            FileOutputStream out = new FileOutputStream(keystoreFile);
            keystore.store(out, password);
            out.close();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    private InputStream fullStream (String fname) throws IOException {
        FileInputStream fis = new FileInputStream(fname);
        DataInputStream dis = new DataInputStream(fis);
        byte[] bytes = new byte[dis.available()];
        dis.readFully(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return bais;
    }


}
