package com.aliware.tianchi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcStatus;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
    static final ConcurrentHashMap<String, Long> rttMap = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, AtomicInteger> activeMap = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, Double> performanceMap = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, Long> threadMap = new ConcurrentHashMap<>();
    static final AtomicBoolean threadChanged = new AtomicBoolean(false);

    private void changPerformanceByThread() {
        if (threadChanged.compareAndSet(true, false)) {
            long totalThread = 0;
            for (Map.Entry<String, Long> entry : threadMap.entrySet()) {
                totalThread += entry.getValue();
            }
            for (Map.Entry<String, Long> entry : threadMap.entrySet()) {
                performanceMap.put(entry.getKey(), 1.0 * totalThread / entry.getValue());
            }
        }
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (invokers.size() == 1) return invokers.get(0);
        if (threadChanged.get()) {
            changPerformanceByThread();
        }
        TreeMap<Long, Invoker> map = new TreeMap<>();
        for (Invoker<T> invoker : invokers) {
            String host = invoker.getUrl().getHost();
            AtomicInteger integer = activeMap.get(host);
            if (integer == null) {
                activeMap.putIfAbsent(host, new AtomicInteger());
                integer = activeMap.get(host);
            }
            int active = integer.get();
            // if (active >= threadMap.getOrDefault(host, 200L)) {
            //     continue;
            // }
            Double performanceTimes = performanceMap.getOrDefault(host, 1.0);
            long rtt = rttMap.getOrDefault(host, 0L);
            map.put((long) (rtt + active * performanceTimes), invoker);
        }
        Map.Entry<Long, Invoker> entry = map.firstEntry();
        if (entry != null) {
            Invoker invoker = entry.getValue();
            activeMap.get(invoker.getUrl().getHost()).incrementAndGet();
            return invoker;
        }
        int pos = ThreadLocalRandom.current().nextInt(invokers.size());
        activeMap.get(invokers.get(pos).getUrl().getHost()).incrementAndGet();
        System.out.println("random");
        return invokers.get(pos);
    }
}