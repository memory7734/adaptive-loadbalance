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
    private final AtomicInteger thread = new AtomicInteger();

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
        status.totalElapsed.addAndGet(Long.valueOf(attrs.get("rt")));
        if (status.thread.get() == 0) {
            status.thread.set(Integer.valueOf(attrs.get("thread")));

        }
    }

    public int getActive() {
        return active.get();
    }

    public int getRemainder() {
        if (thread.get() == 0) return 0;
        return thread.get() - active.get();
    }

    public int getThread() {
        return thread.get();
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

}
