package com;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.util.Properties;

public class Main {

    public static String AbsPath;
    public static Properties prop;

    public static void main(String[] args) {

        try {
            int i = 0;//counter threads


            String path = Main.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");

            AbsPath = decodedPath
                    .replace("bridge-1.0.jar", "");
                    //.replace("target/classes/", "");

            prop = new Properties();


            System.out.println("Приложение размещено : " + AbsPath);
            //System.out.println("config.properties : " + AbsPath + "config.properties");
            FileInputStream input = new FileInputStream(AbsPath + "config.properties");
            prop.load(input);

//            System.out.println("APP_MODE : "+prop.getProperty("APP_MODE"));
//            System.out.println("MQTT_HOST :" + prop.getProperty("MQTT_HOST"));
//            System.out.println("MQTT_LOGIN :" + prop.getProperty("MQTT_LOGIN"));
//            System.out.println("MQT_LOGIN :" + prop.getProperty("MQT_LOGIN"));
//            System.out.println("UART_PORT :" + prop.getProperty("UART_PORT"));

            HTTPService httpService = new HTTPService();
            MQTTService mqttService = new MQTTService(httpService);

            ServerSocket server = new ServerSocket(3777, 0,
                    InetAddress.getByName("localhost"));


            // слушаем порт
            while(true) {

                new UARTServer(i, server.accept(),mqttService,httpService);
                i++;
            }

        } catch(Exception e) {
            e.printStackTrace();
        } catch(Throwable th) {
            th.printStackTrace();
        }
    }

}
