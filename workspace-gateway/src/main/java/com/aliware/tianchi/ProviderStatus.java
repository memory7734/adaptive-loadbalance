package com.aliware.tianchi;

import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListSet;

public class ProviderStatus {

    static ProviderStatus[] providers = new ProviderStatus[3];
    static ConcurrentSkipListSet<ProviderThread> queue = new ConcurrentSkipListSet<>(Comparator.comparing(o -> o.rtt));

    ProviderStatus(int port, int maxThreads) {
        for (int i = 0; i < maxThreads; i++) {
            queue.add(new ProviderThread(port, 50));
        }
    }

    public static void response(int port, long rtt) {
        if (rtt < 3) {
            queue.pollLast();
            queue.add(new ProviderThread(port, rtt));
            queue.add(new ProviderThread(port, 50));
        } else {
            queue.add(new ProviderThread(port, rtt));
        }

    }
}
