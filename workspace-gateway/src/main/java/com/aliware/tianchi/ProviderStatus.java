package com.aliware.tianchi;

import java.util.concurrent.ConcurrentSkipListSet;

public class ProviderStatus {

    static ProviderStatus[] providers = new ProviderStatus[3];
    final int port;
    final int maxThreads;
    static ConcurrentSkipListSet<ProviderThread> queue = new ConcurrentSkipListSet<>((o1, o2) -> o1.rtt.compareTo(o2.rtt));

    public ProviderStatus(int port, int maxThreads) {
        this.port = port;
        this.maxThreads = maxThreads;
    }

    public synchronized void init() {
        for (int i = 0; i < maxThreads; i++) {
            queue.add(new ProviderThread(port, 50));
        }
    }

    static ProviderThread select() {
        return queue.pollFirst();
    }

    static void record(int port, long rtt) {
        providers[(port - 20870) / 10].record(rtt);
    }

    void request(long rtt) {
        queue.add(new ProviderThread(port, rtt));
    }

    private void record(long rtt) {
        if (rtt < 3) {
            queue.pollLast();
            queue.add(new ProviderThread(port, rtt));
        }
        request(50);
    }
}
