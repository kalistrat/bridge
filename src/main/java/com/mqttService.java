package com;

import org.eclipse.paho.client.mqttv3.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.net.ssl.*;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
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

    MqttConnectOptions mqttOptions;
    List<sensorData> sensorList;

    public MQTTService()  {
        try {



            setTopicList();
            setMqttService();


        } catch (Throwable e3){
            System.out.println("MQTT : не удалось запустить службу");
        }

    }

    private void setTopicList(){
        try {

            sensorList  = new ArrayList<>();
            Document xmlSensors = staticMethods.loadXMLFromString(
                    staticMethods.readFile(Main.AbsPath + "topics.xml")
            );

            Node node = (Node) XPathFactory.newInstance().newXPath()
                    .compile("/sensors").evaluate(xmlSensors, XPathConstants.NODE);

            //System.out.println("root node.getTextContent() : " + node.getTextContent());

            NodeList nodeList = node.getChildNodes();

            for (int i=0; i<nodeList.getLength(); i++) {
                NodeList childNodeListSns = nodeList.item(i).getChildNodes();
                for (int j=0; j<childNodeListSns.getLength();j++) {
                    sensorData snsData = null;
                    if (childNodeListSns.item(j).getNodeName().equals("uid")) {
                        snsData = new sensorData(childNodeListSns
                                .item(j).getTextContent());

                        System.out.println("snsData.UID : " + snsData.UID);
                    }


                    if (childNodeListSns.item(j).getNodeName().equals("topics")) {
//                        NodeList topicNodeListSns = childNodeListSns.item(j);
//
//                        for (int k = 0; k < topicNodeListSns.getLength(); k++) {
//
//
//                            String snsDataMeasureType = null;
//                            String snsDataTopicName = null;
//
//                            if (topicNodeListSns.item(k).getNodeName().equals("measure_type")){
//                                snsDataMeasureType = topicNodeListSns
//                                        .item(k).getTextContent();
//                            } else {
//                                System.out.println("topicNodeListSns.item(k).getNodeName() : " + topicNodeListSns.item(k).getNodeName());
//                            }
//
//                            if (topicNodeListSns.item(k).getNodeName().equals("topic_name")){
//                                snsDataTopicName = topicNodeListSns
//                                        .item(k).getTextContent();
//                            }
//
//
//                            System.out.println("snsDataMeasureType : " + snsDataMeasureType);
//                            System.out.println("snsDataTopicName : " + snsDataTopicName);
//
//                        }
                    }
                    //sensorList.add(snsData);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("MQTT : Ошибка чтения файла топиков");
        }

    }

    public void setMqttService(){
        try {

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
            e1.printStackTrace();
            System.out.println("MQTT : центральный сервер недоступен или неверен логин и пароль");
            readClient = null;
        }
    }

    public void startMqttPublisher() {
        try{

            writeClient = null;
            System.gc();
            System.out.println("writeClient start create");
            writeClient = new MqttClient(mqttServerHost, MqttClient.generateClientId(), null);

            MqttMessage mqttMessage = new MqttMessage("test_publisher".getBytes());
            System.out.println("try to connect to mqtt-server");
            writeClient.connect(mqttOptions);
            writeClient.publish("TEST", mqttMessage);
            writeClient.disconnect();

            System.out.println("writeClient done");

        } catch (MqttException e1) {
            System.out.println("MQTT : центральный сервер недоступен или неверен логин и пароль");
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

    public void addInTopicList(String uid,String topic){
//        try {
//            FileOutputStream output = new FileOutputStream(Main.AbsPath + "topics.properties");
//            topicProp.put(uid, topic);
//            topicProp.store(output, null);
//        } catch (IOException e){
//            e.printStackTrace();
//        }
    }

    private void deleteFromTopicList(){
//        String file="D:\\path of your file\abc.properties";
//        Path path = Paths.get(file);
//        Charset charset = StandardCharsets.UTF_8;
//
//        String content = new String(Files.readAllBytes(path), charset);
//        content = content.replaceAll("name=anything", "name=anything1");
//        Files.write(path, content.getBytes(charset));
    }

}
