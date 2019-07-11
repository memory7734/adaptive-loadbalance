package com.aliware.tianchi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

public class Status {
    private static final ConcurrentMap<Integer, Status> SERVICE_STATISTICS = new ConcurrentHashMap<>();

    private final LongAdder active = new LongAdder();
    private long total = 0;
    private int thread = 0;
    private int canUseActive = 0;
    private long[] elapsed = new long[1024];
    private long[] avgElapsed = new long[1024];

    public static Status getStatus(Integer port) {
        Status status = SERVICE_STATISTICS.get(port);
        if (status == null) {
            SERVICE_STATISTICS.putIfAbsent(port, new Status());
            status = SERVICE_STATISTICS.get(port);
        }
        return status;
    }


    public static void beginCount(Integer port) {
        Status status = getStatus(port);
        status.active.increment();
        status.canUseActive--;
    }

    public static void endCount(Integer port, Map<String, String> attrs, boolean hasException) {
        Status status = getStatus(port);
        status.active.decrement();
        int p = (int) ((status.total + 1) & 1023);
        status.total++;
        status.canUseActive++;
        if (status.thread == 0) {
            status.thread = Integer.valueOf(attrs.get("thread"));
        }
        status.elapsed[p] = hasException ? 1000 : Long.valueOf(attrs.get("rt"));
        if (status.elapsed[p] > status.avgElapsed[p] * 3) {
            status.elapsed[p] = 1000;
        }
    }

    public int getActive() {
        return active.intValue();
    }

    public int getRemainder() {
        if (thread == 0) {
            return 0;
        }
        return thread - getActive();
    }

    // 如果可用的线程数量低于线程总数的一半，则返回0
    public int getCanUseRemainder() {
        if (canUseActive <= 0 || canUseActive >= thread / 2) {
            canUseActive = Math.max(0, thread / 2 - getActive());
        }
        return canUseActive;
    }

    public long getAvgElapsed() {
        int p = (int) (total & 1023);
        if (elapsed[p] > 800) {
            return 1000;
        }
        long sum = 0;
        for (long a : elapsed) {
            sum += a;
        }
        avgElapsed[p] = sum / 1024;
        return avgElapsed[p];
    }
}
