package com.aliware.tianchi;

import org.apache.dubbo.common.URL;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Status {
    private static final ConcurrentMap<String, Status> SERVICE_STATISTICS = new ConcurrentHashMap<>();

    private final AtomicInteger active = new AtomicInteger();
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong totalElapsed = new AtomicLong();

    public static Status getStatus(URL url) {
        String uri = url.toIdentityString();
        Status status = SERVICE_STATISTICS.get(uri);
        if (status == null) {
            SERVICE_STATISTICS.putIfAbsent(uri, new Status());
            status = SERVICE_STATISTICS.get(uri);
        }
        return status;
    }


    public static void beginCount(URL url) {
        Status appStatus = getStatus(url);
        appStatus.active.incrementAndGet();
    }

    public static void endCount(URL url, long elapsed, boolean succeeded) {
        Status status = getStatus(url);
        status.active.decrementAndGet();
        status.total.incrementAndGet();
        status.totalElapsed.addAndGet(elapsed);
    }

    public int getActive() {
        return active.get();
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

    public static void resetElapsed(URL url) {
        Status status = getStatus(url);
        status.totalElapsed.set(0);
        status.total.set(0);
    }

}
