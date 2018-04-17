package com;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by kalistrat on 12.04.2018.
 */
public class UARTService implements UARTListenable {
    ScheduledExecutorService ses;
    int taskInterval = 2;
    UARTListener uartListener;

    public UARTService(){

        try {
            ses =
                    Executors.newScheduledThreadPool(1);
            Runnable pinger = new Runnable() {
                public void run() {
                    System.out.println("123");
                    uartListener.uartMessageSent("SEN-R0LS9VWSC2EN:123");
                }
            };

            ses.scheduleAtFixedRate(pinger, 20, taskInterval, TimeUnit.SECONDS);

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void addUARTListener(UARTListener listener){
        this.uartListener = listener;
    }
}
