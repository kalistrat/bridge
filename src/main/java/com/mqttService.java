package com;

import org.eclipse.paho.client.mqttv3.*;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Created by kalistrat on 13.04.2018.
 */
public class mqttService implements MqttCallback {

    MqttClient client;
    String serverLogin;
    String serverPassword;
    String mqttServerHost;
    String readTopicName;

    public mqttService() throws Throwable {

        try{

            client = new MqttClient(mqttServerHost, MqttClient.generateClientId(), null);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(serverLogin);
            options.setPassword(serverPassword.toCharArray());

            if (mqttServerHost.contains("ssl://")) {
                SSLSocketFactory ssf = configureSSLSocketFactory();
                options.setSocketFactory(ssf);
            }

            options.setConnectionTimeout(0);

            client.connect(options);
            client.setCallback(this);
            client.subscribe(readTopicName);

        } catch (MqttException e1) {
            //System.out.println("MqttException in ConditionVariable");
            throw  e1;
        }

    }

    public SSLSocketFactory configureSSLSocketFactory() throws KeyManagementException, NoSuchAlgorithmException,
            UnrecoverableKeyException, IOException, CertificateException, KeyStoreException {
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream jksInputStream = new FileInputStream(Main.AbsPath + "clientkeystore.jks");
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

}
