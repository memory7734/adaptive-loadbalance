package com.aliware.tianchi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

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
    static long[] tpsArray = new long[3];
    static long[] weightArray = new long[3];
    // static long[] lastRttArray = new long[3];
    // static long[] succeededTaskArray = new long[3];
    // static long[] failedTaskArray = new long[3];
    // static boolean[] catchExceptionArray = new boolean[3];
    // static long[] requestLimitTime = new long[3];

    // static boolean activeChanged = false;

    private <T> Invoker<T> selectByThread(List<Invoker<T>> invokers) {
        int sum = 0;
        for (int i = 0; i < remainderArray.length; i++) {
            if (remainderArray[i] < (threadArray[i] / 2) || remainderArray[i] <= 0) continue;
            sum += remainderArray[i];
        }
        if (sum > 0) {
            int offset = ThreadLocalRandom.current().nextInt(sum);
            for (Invoker<T> invoker : invokers) {
                int index = (invoker.getUrl().getPort() - 20870) / 10;
                if (remainderArray[index] < threadArray[index] / 2 || remainderArray[index] <= 0) continue;
                offset -= remainderArray[index];
                if (offset < 0) return invoker;
            }
        }
        return null;
    }

    private <T> Invoker<T> selectByTps(List<Invoker<T>> invokers) {
        long sum = 0;
        for (long x : weightArray) {
            sum += x;
        }
        if (sum > 0) {
            long offset = ThreadLocalRandom.current().nextLong(sum);
            for (Invoker<T> invoker : invokers) {
                int index = (invoker.getUrl().getPort() - 20870) / 10;
                offset -= weightArray[index];
                if (offset < 0) return invoker;
            }
        }
        return null;
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        return CompletableFuture.supplyAsync(() -> {
            Invoker<T> result;
            result = selectByThread(invokers);
            if (result == null) {
                result = selectByTps(invokers);
            }
            if (result == null) {
                result = invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
            }
            int index = (result.getUrl().getPort() - 20870) / 10;
            remainderArray[index]--;
            weightArray[index] -= tpsArray[index];
            return result;
        }).join();

        // System.out.println(remainderArray[index] + "/" + threadArray[index] + "/" + tpsArray[index] + "/" + lastRttArray[index]);
        // return result;
    }
}