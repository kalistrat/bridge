package com;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kalistrat on 18.04.2018.
 */
public class UARTServer extends Thread {

    Socket s;
    int num;
    MQTTService mqttService;
    HTTPService httpService;

    public UARTServer(int num, Socket s, MQTTService mService,HTTPService hService) {
        // копируем данные
        this.num = num;
        this.s = s;
        this.mqttService = mService;
        this.httpService = hService;

        // и запускаем новый вычислительный поток (см. ф-ю run())
        setDaemon(true);
        setPriority(NORM_PRIORITY);
        start();
    }

    public void run() {
        try {
            // из сокета клиента берём поток входящих данных
            InputStream is = s.getInputStream();
            // и оттуда же — поток данных от сервера к клиенту
            OutputStream os = s.getOutputStream();
            // буффер данных в 256 килобайта
            byte buf[] = new byte[256*1024];
            // читаем 64кб от клиента, результат —
            // кол-во реально принятых данных
            int r = is.read(buf);

            String ClientData = new String(buf, 0, r);
            // выводим данные:

            os.write(executeSnsMessAge(ClientData).getBytes("UTF8"));
            // завершаем соединение
            s.close();
        }
        catch(Exception e) {
            System.out.println("Ошибка: "+e);
        } // вывод исключений
    }

    private String executeSnsMessAge(String clientMessage){
        String result;
        try {

            List<String> messPieces = staticMethods.getListFromString(clientMessage,":");
            //UID:TIME(unix):VALUES:STATE
            //SNS-123123:42141231341:34.3:5

            if (messPieces.size() == 1) {
                String uid = getUIDSensor(messPieces.get(0));
                if (uid != null) {
                    String preffixUID = staticMethods.getListFromString(uid,"-").get(0);
                    if (preffixUID.equals("SEN")) {
                        if (mqttService.writeClient != null) {
                            if (httpService.httpClient != null) {

                                String httpResponse = httpService.linkDevice(mqttService.mqttUID + "|" + uid);
                                if (!httpResponse.contains("ERROR")) {
                                    String senTopic = staticMethods.getResponseAttrValue("toServerTopic", httpResponse);
                                    sensorData newSns = mqttService.addSensor(uid);
                                    newSns.addTopic("SEN_TYPE", senTopic);
                                    mqttService.addSensorToFile(newSns);
                                    mqttService.createMessageToQueue(mqttService.writeTopicName,"STATE:" + uid + ":CONNECTED:" + staticMethods.unixTime());
                                    System.out.println("UART : датчик с " + uid + " успешно привязан");
                                    result = "key : XXX";
                                } else {
                                    System.out.println("UART : датчик с " + uid + " привязать не удалось : " + httpResponse);
                                    result = "message : " + clientMessage + " - accepted and processed";
                                }

                            } else {
                                System.out.println("UART : служба привязки (http) недоступна. Производится её повторный запуск");
                                result = "message : " + clientMessage + " - accepted and processed";
                                httpService.setHttpService();
                            }
                        } else {
                            System.out.println("UART : служба отправки показаний (mqtt) не доступна. Производится её повторный запуск");
                            mqttService.setMqttService();
                            result = "message : " + clientMessage + " - accepted and processed";
                        }

                    } else {
                        System.out.println("UART : привязываемый датчик имеет недопустимый тип -" + preffixUID);
                        result = "message : " + clientMessage + " - accepted and processed";
                    }

                } else {
                    System.out.println("UART : UID привязываемого датчика имеет недопустимый формат");
                    result = "message : " + clientMessage + " - accepted and processed";
                }
            } else if (messPieces.size() == 4) {
                String uid = getUIDSensor(messPieces.get(0));
                if (uid != null) {
                    if (mqttService.isSensorLinked(uid)) {
                        if (mqttService.writeClient != null) {
                            mqttService.addMessageToQueue(uid, messPieces);
                            System.out.println("UART : Сообщение " + clientMessage + " - успешно отправлено");
                            result = "message : " + clientMessage + " - accepted and processed";
                        } else {
                            System.out.println("UART : служба отправки показаний (mqtt) не доступна. Производится её повторный запуск");
                            mqttService.setMqttService();
                            result = "message : " + clientMessage + " - accepted and processed";
                        }
                    } else {
                        System.out.println("UART : датчик с UID " + uid + " - не привязан. Сообщение не может быть отправлено");
                        result = "message : " + clientMessage + " - accepted and processed";
                    }
                } else {
                    System.out.println("UART : принятое сообщение имеет недопустимый UID и не может быть обработано");
                    result = "message : " + clientMessage + " - accepted and processed";
                }
            } else {
                System.out.println("UART : принятое сообщение имеет недопустимый формат");
                result = "message : " + clientMessage + " - accepted and processed";
            }


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("UART : при обработке сообщения произошла ошибка выполнения");
            result = "message : " + clientMessage + " - accepted and processed";;
        }

        return result;
    }


    private String getUIDSensor(String s){
        String UID = null;
        try {
            String regex = "^(BRI{1}|SEN{1}|RET{1})-[a-zA-Z0-9]{12}$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(s);

            if (matcher.matches()){
                UID = matcher.group(0);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return UID;
    }

}
