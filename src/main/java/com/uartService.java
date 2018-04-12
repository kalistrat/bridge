package com;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by kalistrat on 12.04.2018.
 */
public class uartService {
    ScheduledExecutorService ses;
    int taskInterval = 2;

    public uartService(){

        try {
            ses =
                    Executors.newScheduledThreadPool(1);
            Runnable pinger = new Runnable() {
                public void run() {
                    System.out.println("123");
                }
            };

            ses.scheduleAtFixedRate(pinger, 0, taskInterval, TimeUnit.SECONDS);

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
