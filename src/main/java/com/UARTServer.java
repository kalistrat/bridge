package com;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
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

    public UARTServer(int num, Socket s, MQTTService mService) {
        // копируем данные
        this.num = num;
        this.s = s;
        this.mqttService = mService;

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

            List<String> messPieces = getListFromString(clientMessage,":");

            if (messPieces.size() == 1) {
                String uid = getUIDSensor(messPieces.get(0));
                if (uid != null) {
                    result = "this is encription key";
                } else {
                    result = "invalid message UID";
                }
            } else if (messPieces.size() == 2) {
                String uid = getUIDSensor(messPieces.get(0));
                if (uid != null) {
                    if (mqttService.writeClient != null) {
                        mqttService.publishUIDMessage(uid,messPieces.get(1));
                        result = "message sent to mqtt server : " + mqttService.mqttServerHost;
                    } else {
                        result = "Server : " + mqttService.mqttServerHost +" is not available";
                    }
                } else {
                    result = "invalid message UID";
                }
            } else {
                result = "invalid sensor message";
            }


        } catch (Exception e) {
            e.printStackTrace();
            result = "Error execution executeSnsMessAge";
        }

        return result;
    }

    private List<String> getListFromString(String DevidedString, String Devider){
        List<String> StrPieces = new ArrayList<String>();
        try {
            int k = 0;
            String iDevidedString;
            // 123|321|456|

            if (DevidedString.startsWith(Devider)) {
                DevidedString = DevidedString.substring(1,DevidedString.length());
            }

            if (!DevidedString.contains(Devider)) {
                iDevidedString = DevidedString + Devider;
            } else {
                if (!DevidedString.endsWith(Devider)) {
                    iDevidedString = DevidedString + Devider;
                } else {
                    iDevidedString = DevidedString;
                }
            }

            while (!iDevidedString.equals("")) {
                int Pos = iDevidedString.indexOf(Devider);
                StrPieces.add(iDevidedString.substring(0, Pos));
                iDevidedString = iDevidedString.substring(Pos + 1);
                k = k + 1;
                if (k > 100000) {
                    iDevidedString = "";
                }
            }

        } catch (Exception e){

        }

        return StrPieces;
    }

    private String getUIDSensor(String s){
        String UID = null;
        try {
            String regex = "^(BRI{1}|SNS{1}|RET{1})-[a-zA-Z0-9]{10}$";
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
