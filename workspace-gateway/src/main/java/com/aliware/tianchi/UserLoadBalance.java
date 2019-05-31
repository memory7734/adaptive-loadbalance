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

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (invokers.size() == 1) return invokers.get(0);
        TreeMap<Long, Invoker> map = new TreeMap<>();
        for (Invoker<T> invoker : invokers) {
            String urlStringKey = invoker.getUrl().setHost(invoker.getUrl().getIp()).toIdentityString();
            AtomicInteger integer = activeMap.get(urlStringKey);
            if (integer == null) {
                activeMap.putIfAbsent(urlStringKey, new AtomicInteger());
                integer = activeMap.get(urlStringKey);
            }
            int active = integer.get();
            if (active >= threadMap.getOrDefault(urlStringKey, 200L) * 0.9) {
                continue;
            }
            Double performanceTimes = performanceMap.getOrDefault(urlStringKey, 1.0);
            long rtt = rttMap.getOrDefault(urlStringKey, 0L);
            map.put((long) (rtt + active * performanceTimes), invoker);
        }
        Map.Entry<Long, Invoker> entry = map.firstEntry();
        if (entry != null) {
            Invoker invoker = entry.getValue();
            activeMap.get(invoker.getUrl().setHost(invoker.getUrl().getIp()).toIdentityString()).incrementAndGet();
            return invoker;
        }
        int pos = ThreadLocalRandom.current().nextInt(invokers.size());
        activeMap.get(invokers.get(pos).getUrl().toIdentityString()).incrementAndGet();
        return invokers.get(pos);
    }
}