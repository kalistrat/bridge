package com;

import java.net.URLDecoder;

public class Main {

    public static String AbsPath;

    public static void main(String[] args) {

        try {


            String path = Main.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");

            AbsPath = decodedPath
                    .replace("bridge-1.0.jar", "")
                    .replace("target/classes/", "");


            System.out.println("AbsPath : " + AbsPath);


            uartService eS = new uartService();

        } catch(Exception e) {
            e.printStackTrace();
        } catch(Throwable th) {
            th.printStackTrace();
        }
    }
}
