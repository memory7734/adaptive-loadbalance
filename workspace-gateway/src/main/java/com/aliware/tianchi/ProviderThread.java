package com.aliware.tianchi;

import java.util.concurrent.ThreadLocalRandom;

public class ProviderThread {
    final int port;
    final Double rtt;

    public ProviderThread(int port, double rtt) {
        this.port = port;
        this.rtt = rtt + ThreadLocalRandom.current().nextDouble();
    }
}
