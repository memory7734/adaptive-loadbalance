package com.aliware.tianchi;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CurrentTime {
    public static long current = System.currentTimeMillis();
    private static ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    static {
        service.scheduleAtFixedRate(() -> current = System.currentTimeMillis(), 0, 1, TimeUnit.MILLISECONDS);
    }
}
