package com.aliware.tianchi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class Status {
    private static final ConcurrentMap<Integer, Status> SERVICE_STATISTICS = new ConcurrentHashMap<>();

    private final LongAdder active = new LongAdder();
    private long total = 0;

    private final static int[] portArray = {20870, 20880, 20890};
    private final static int RT_SIZE = 1024;
    private static int totalThread = 0;
    private int canUseThread = 0;
    private int thread = 0;

    private static int current = 0;
    private int canUseActive = 0;
    private long[] elapsed = new long[RT_SIZE];
    private double avgElapsed = 0;


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
        if (Status.current < Status.totalThread) {
            Status.current++;
        }
        if (Status.current >= Status.totalThread * 0.9) {
            Status.refreshCurrent();
        }
    }

    public static void endCount(Integer port, Map<String, String> attrs, boolean hasException) {
        Status status = getStatus(port);
        status.active.decrement();
        int p = (int) ((status.total + 1) & RT_SIZE - 1);
        status.total++;
        status.canUseActive++;
        if (status.thread == 0) {
            status.thread = Integer.valueOf(attrs.get("thread"));
            Status.totalThread += status.thread;
            status.canUseThread = status.thread - 200;
            if (status.canUseThread == 0) {
                status.canUseThread = 50;
            }
            status.canUseActive = status.canUseThread;
        }
        long lastElapsed = status.elapsed[p];
        long lastRt = hasException ? 1000 : Long.valueOf(attrs.get("rt"));
        status.avgElapsed = (status.avgElapsed * RT_SIZE - lastElapsed + lastRt) / RT_SIZE;
        status.elapsed[p] = lastRt;
        if (Status.current > 0) {
            Status.current--;
        } else {
            Status.refreshCurrent();
        }
    }

    public int getActive() {
        return active.intValue();
    }

    public int getRemainder() {
        // if (thread == 0) {
        //     return 0;
        // }
        return thread - getActive();
    }


    // 如果可用的线程数量低于线程总数的一半，则返回0
    public int getCanUseRemainder() {
        if (canUseActive <= 20 || canUseActive >= canUseThread * 0.9) {
            refreshCurrent();
        }
        return canUseActive;
    }

    public long getLastElapsed() {
        return elapsed[(int) ((total + 1) & RT_SIZE - 1)];
    }

    public static long getTotalAvgElapsed() {
        return (long) Math.min(Math.min(getStatus(20870).avgElapsed, getStatus(20880).avgElapsed), getStatus(20890).avgElapsed);
    }

    public double getAvgElapsed() {
        return avgElapsed;
    }

    public static int getCurrent() {
        return current;
    }

    public double getCurrentRt() {
        return 10000 / avgElapsed;
    }

    public static double getAvgRt() {
        int sum = 0;
        Status status = getStatus(20870);
        if (status.avgElapsed > 0) {
            sum += 10000 / status.avgElapsed;
        }
        status = getStatus(20880);
        if (status.avgElapsed > 0) {
            sum += 10000 / status.avgElapsed;
        }
        status = getStatus(20890);
        if (status.avgElapsed > 0) {
            sum += 10000 / status.avgElapsed;
        }
        return sum;
    }

    public static void setCurrent(int current) {
        Status.current = current;
    }

    public static void refreshCurrent() {
        int sum = 0;
        for (int port : portArray) {
            Status status = Status.getStatus(port);
            int temp = Math.max(0, status.canUseThread - status.active.intValue());
            status.canUseActive = temp;
            sum += temp;
            // status.avgElapsed = 50;

            // for (int i = 0; i < status.avgElapsed.length; i++) {
            //     status.avgElapsed[i] = 50;
            // }
        }
        Status.setCurrent(sum);
    }
}
