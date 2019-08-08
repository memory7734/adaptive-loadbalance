package com.aliware.tianchi;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CurrentTime {
    public static long current = System.currentTimeMillis();
    private static ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private static Runnable runnable = new Runnable() {
        @Override
        public void run() {
            current = System.currentTimeMillis();
        }
    };
    static{
        service.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.MILLISECONDS);
    }
}
