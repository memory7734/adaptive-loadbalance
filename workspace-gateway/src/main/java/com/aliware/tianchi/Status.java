package com.aliware.tianchi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

public class Status {
    private static final ConcurrentMap<Integer, Status> SERVICE_STATISTICS = new ConcurrentHashMap<>();

    private int active = 0;
    private long total = 0;
    private int thread = 0;
    private long[] elapsed = {200, 200, 200};

    public static Status getStatus(Integer port) {
        Status status = SERVICE_STATISTICS.get(port);
        if (status == null) {
            SERVICE_STATISTICS.putIfAbsent(port, new Status());
            status = SERVICE_STATISTICS.get(port);
        }
        return status;
    }


    public static void update(Integer port, Map<String, String> attrs, boolean hasException) {
        Status status = getStatus(port);
        status.total++;
        if (status.thread == 0) {
            status.thread = Integer.valueOf(attrs.get("thread"));
        }
        status.active = Integer.valueOf(attrs.get("active"));
        status.elapsed[(int) (status.total % 3)] = hasException ? 1000 : Long.valueOf(attrs.get("rt"));
    }

    public int getActive() {
        return active;
    }

    public int getRemainder() {
        if (thread == 0) {
            return 0;
        }
        return thread - active;
    }
    // 如果可用的线程数量低于线程总数的一半，则返回0
    public int getCanUseRemainder() {
        int temp = thread / 2 - active;
        return temp > 0 ? temp : 0;
    }

    public long getElapsed() {
        return (elapsed[0] + elapsed[1] + elapsed[2]) / 3;
    }
}
