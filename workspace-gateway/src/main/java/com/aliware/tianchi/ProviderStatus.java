package com.aliware.tianchi;

import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListSet;

public class ProviderStatus {

    static ProviderStatus[] providers = new ProviderStatus[3];
    static ConcurrentSkipListSet<ProviderThread> queue = new ConcurrentSkipListSet<>(Comparator.comparing(o -> o.rtt));

    final int port;
    final int maxThreads;

    public ProviderStatus(int port, int maxThreads) {
        this.port = port;
        this.maxThreads = maxThreads;
        for (int i = 0; i < maxThreads; i++) {
            queue.add(new ProviderThread(port, 50));
        }
    }

    public void response(long rtt) {
        if (rtt < 10) {
            queue.pollLast();
            queue.add(new ProviderThread(port, rtt));
        }
        queue.add(new ProviderThread(port, rtt));
    }
}
