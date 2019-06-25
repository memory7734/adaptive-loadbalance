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
    static int[] threadArray = new int[3];
    static long[] averageRttArray = {500, 500, 500};
    static long[] lastRttArray = {1000, 1000, 1000};
    static AtomicInteger[] remainderArray = {new AtomicInteger(), new AtomicInteger(), new AtomicInteger()};
    private AtomicInteger total = new AtomicInteger();

    static final AtomicBoolean activeChanged = new AtomicBoolean(false);

    private <T> Invoker<T> selectByThread(List<Invoker<T>> invokers) {
        if (activeChanged.get()) {
            if (activeChanged.compareAndSet(true, false)) {
                int sum = 0;
                for (int i = 0; i < 3; i++) {
                    sum += remainderArray[i].get();
                }
                total.set(sum);
            }
        }
        if (total.get() > 0) {
            int offset = ThreadLocalRandom.current().nextInt(total.get());
            for (Invoker<T> invoker : invokers) {
                int index = (invoker.getUrl().getPort() - 20870) / 10;
                offset -= remainderArray[index].get();
                if (offset < 0) {
                    remainderArray[index].getAndDecrement();
                    total.getAndDecrement();
                    return invoker;
                }
            }
        }
        return null;
    }

    private <T> Invoker<T> selectByRtt(List<Invoker<T>> invokers) {
        Invoker<T> result = null;
        long minRtt = Long.MAX_VALUE;
        for (Invoker<T> invoker : invokers) {
            int index = (invoker.getUrl().getPort() - 20870) / 10;
            if (lastRttArray[index] >= averageRttArray[index] * 3) {
                continue;
            }
            if (minRtt > averageRttArray[index]) {
                minRtt = averageRttArray[index];
                result = invoker;
            }
        }
        return result;
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        Invoker<T> invoker = selectByThread(invokers);
        if (invoker != null) {
            int index = (invoker.getUrl().getPort() - 20870) / 10;
            if (lastRttArray[index] < averageRttArray[index] * 3) return invoker;
        }
        invoker = selectByRtt(invokers);
        if (invoker == null) {
            invoker = invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
        }
        int index = (invoker.getUrl().getPort() - 20870) / 10;
        total.getAndDecrement();
        remainderArray[index].getAndDecrement();
        return invoker;
    }
}