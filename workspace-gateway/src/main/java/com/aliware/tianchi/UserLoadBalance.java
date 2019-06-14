package com.aliware.tianchi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author daofeng.xjf
 * <p>
 * 负载均衡扩展接口
 * 必选接口，核心接口
 * 此类可以修改实现，不可以移动类或者修改包名
 * 选手需要基于此类实现自己的负载均衡算法
 */
public class UserLoadBalance implements LoadBalance {

    static final ConcurrentHashMap<String, Integer> threadMap = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, AtomicInteger> remainderMap = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, Long> rttMap = new ConcurrentHashMap<>();
    static final AtomicBoolean activeChanged = new AtomicBoolean(false);
    private static final AtomicInteger totalRemainder = new AtomicInteger(0);
    private static final AtomicInteger zero = new AtomicInteger(0);

    private void changPerformanceByThread() {
        if (activeChanged.compareAndSet(true, false)) {
            int total = 0;
            for (Map.Entry<String, AtomicInteger> entry : remainderMap.entrySet()) {
                total += entry.getValue().get();
            }
            totalRemainder.set(total);
        }
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (activeChanged.get()) {
            changPerformanceByThread();
        }
        int total = totalRemainder.get();
        if (total > 0) {
            int offset = ThreadLocalRandom.current().nextInt(total);
            for (Invoker<T> invoker : invokers) {
                String host = invoker.getUrl().getHost();
                AtomicInteger remainder = remainderMap.getOrDefault(host, zero);
                offset -= remainder.get();
                if (offset < 0) {
                    remainder.getAndDecrement();
                    totalRemainder.getAndDecrement();
                    return invoker;
                }
            }
        }
        long rttTotal = 0;
        for (Map.Entry<String, Long> entry : rttMap.entrySet()) {
            rttTotal += entry.getValue();
        }
        if (rttMap.size() > 2) {
            long offset = ThreadLocalRandom.current().nextLong(rttTotal * (rttMap.size() - 1));
            for (Invoker<T> invoker : invokers) {
                offset -= rttTotal - rttMap.getOrDefault(invoker.getUrl().getHost(), 0L);
                if (offset < 0) {
                    totalRemainder.getAndDecrement();
                    return invoker;
                }
            }
        }

        Invoker<T> invoker = invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
        AtomicInteger integer = remainderMap.get(invoker.getUrl().getHost());
        if (integer != null) integer.getAndDecrement();
        totalRemainder.getAndDecrement();
        return invoker;
    }
}