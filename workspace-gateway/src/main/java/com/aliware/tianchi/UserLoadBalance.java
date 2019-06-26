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
    static int[] remainderArray = new int[3];
    static long[] avgRttArray = new long[3];
    static long[] lastRttArray = new long[3];
    static long[] succeededTaskArray = new long[3];
    static long[] failedTaskArray = new long[3];
    static boolean[] catchExceptionArray = new boolean[3];
    static long[] requestLimitTime = new long[3];
    private int total = 0;

    static boolean activeChanged = false;

    private <T> Invoker<T> selectByThread(List<Invoker<T>> invokers) {
        if (activeChanged) {
            int sum = 0;
            for (int x : remainderArray) {
                sum += x;
            }
            total = sum;
        }
        if (total > 0) {
            int offset = ThreadLocalRandom.current().nextInt(total);
            for (Invoker<T> invoker : invokers) {
                int index = (invoker.getUrl().getPort() - 20870) / 10;
                if (remainderArray[index] < (threadArray[index] >> 1)) continue;
                offset -= remainderArray[index];
                if (offset < 0) return invoker;
            }
        }
        return null;
    }

    private <T> Invoker<T> selectByRtt(List<Invoker<T>> invokers) {
        Invoker<T> result = null;
        long minRtt = Long.MAX_VALUE;
        for (Invoker<T> invoker : invokers) {
            int i = (invoker.getUrl().getPort() - 20870) / 10;
            if (avgRttArray[i] == 0) {
                result = null;
                break;
            }
            if (avgRttArray[i] * 2 < lastRttArray[i]) continue;
            if (System.currentTimeMillis() - requestLimitTime[i] <= 5) continue;
            if (avgRttArray[i] < minRtt) {
                minRtt = avgRttArray[i];
                result = invoker;
            }
        }
        return result;
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        Invoker<T> result;
        result = selectByThread(invokers);
        if (result == null) {
            selectByRtt(invokers);
        }
        if (result == null) {
            result = invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
            System.out.println("随机选择结果");
        }
        int index = (result.getUrl().getPort() - 20870) / 10;
        remainderArray[index]--;
        total--;
        return result;
    }
}