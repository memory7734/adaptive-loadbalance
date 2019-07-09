package com.aliware.tianchi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Status {
    private static final ConcurrentMap<Integer, Status> SERVICE_STATISTICS = new ConcurrentHashMap<>();

    private final AtomicInteger active = new AtomicInteger();
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong totalElapsed = new AtomicLong();
    private int thread = 0;
    private long lastElapsed = 200;

    public static Status getStatus(Integer port) {
        Status status = SERVICE_STATISTICS.get(port);
        if (status == null) {
            SERVICE_STATISTICS.putIfAbsent(port, new Status());
            status = SERVICE_STATISTICS.get(port);
        }
        return status;
    }


    public static void beginCount(Integer port) {
        Status appStatus = getStatus(port);
        appStatus.active.incrementAndGet();
    }

    public static void endCount(Integer port, Map<String, String> attrs) {
        Status status = getStatus(port);
        status.active.decrementAndGet();
        status.total.incrementAndGet();
        long rt = Long.valueOf(attrs.get("rt"));
        status.totalElapsed.addAndGet(rt);
        if (status.thread == 0) {
            status.thread = Integer.valueOf(attrs.get("thread"));
        }
        status.lastElapsed = rt;
    }

    public int getActive() {
        return active.get();
    }

    public int getRemainder() {
        if (thread == 0) {
            return 0;
        }
        return thread - getActive();
    }
    // 如果可用的线程数量低于线程总数的一半，则返回0
    public int getCanUseRemainder() {
        if (thread == 0 || getActive() > thread / 2) {
            return 0;
        }
        return thread / 2 - getActive();
    }

    public int getThread() {
        return thread;
    }

    public long getTotal() {
        return total.longValue();
    }

    public long getTotalElapsed() {
        return totalElapsed.get();
    }


    public long getAverageElapsed() {
        long total = getTotal();
        if (total == 0) {
            return 0;
        }
        return getTotalElapsed() / total;
    }


    public long getAverageTps() {
        if (getTotalElapsed() >= 1000L) {
            return getTotal() / (getTotalElapsed() / 1000L);
        }
        return getTotal();
    }

    public static void resetElapsed(Integer port) {
        Status status = getStatus(port);
        status.totalElapsed.set(0);
        status.total.set(0);
    }

    public void setLastElapsed(long lastElapsed) {
        this.lastElapsed = lastElapsed;
    }

    public long getLastElapsed() {
        return lastElapsed;
    }
}
