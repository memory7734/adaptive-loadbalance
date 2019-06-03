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
    static final ConcurrentHashMap<String, Integer> remainderMap = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, Long> rttMap = new ConcurrentHashMap<>();
    static final AtomicBoolean activeChanged = new AtomicBoolean(false);
    private static int totalRemainder = 0;

    private void changPerformanceByThread() {
        if (activeChanged.compareAndSet(true, false)) {
            int total = 0;
            for (Map.Entry<String, Integer> entry : remainderMap.entrySet()) {
                total += entry.getValue();
            }
            totalRemainder = total;
        }
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (activeChanged.get()) {
            changPerformanceByThread();
        }
        if (totalRemainder > 0) {
            int offset = ThreadLocalRandom.current().nextInt(totalRemainder);
            for (Invoker<T> invoker : invokers) {
                String host = invoker.getUrl().getHost();
                offset -= remainderMap.getOrDefault(host, 0);
                if (offset < 0) {
                    return invoker;
                }
            }
        }
        return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
    }
}