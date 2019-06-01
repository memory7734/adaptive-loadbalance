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

    static final ConcurrentHashMap<String, Integer> threadMap = new ConcurrentHashMap<>();
    static final AtomicBoolean threadChanged = new AtomicBoolean(false);
    private static final AtomicInteger totalThread = new AtomicInteger();

    private void changPerformanceByThread() {
        if (threadChanged.compareAndSet(true, false)) {
            int total = 0;
            for (Map.Entry<String, Integer> entry : threadMap.entrySet()) {
                total += entry.getValue();
            }
            totalThread.set(total);
        }
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (threadChanged.get()) {
            changPerformanceByThread();
        }
        if (totalThread.get() > 0) {
            int offset = ThreadLocalRandom.current().nextInt(totalThread.get());
            for (Invoker<T> invoker : invokers) {
                String host = invoker.getUrl().getHost();
                offset -= threadMap.getOrDefault(host, 200);
                if (offset < 0) {
                    return invoker;
                }

            }
        }
        return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
    }
}