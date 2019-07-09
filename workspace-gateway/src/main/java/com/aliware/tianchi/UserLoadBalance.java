package com.aliware.tianchi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author daofeng.xjf
 *
 * 负载均衡扩展接口
 * 必选接口，核心接口
 * 此类可以修改实现，不可以移动类或者修改包名
 * 选手需要基于此类实现自己的负载均衡算法
 */
public class UserLoadBalance implements LoadBalance {

    static int total = 0;


    public static void calcTotal() {

        total = (Status.getStatus(20870).getRemainder()
                + Status.getStatus(20880).getRemainder()
                + Status.getStatus(20890).getRemainder());

    }

    private <T> Invoker<T> selectByThread(List<Invoker<T>> invokers) {
        int sum = total;
        if (sum > 0) {
            int offset = ThreadLocalRandom.current().nextInt(sum / 2);
            for (Invoker<T> invoker : invokers) {
                offset -= Status.getStatus(invoker.getUrl().getPort()).getCanUseRemainder();
                if (offset < 0) return invoker;
            }
        }
        return null;
    }

    private <T> Invoker<T> selectByTps(List<Invoker<T>> invokers) {
        Invoker<T> result = null;
        long minRt = Long.MAX_VALUE;
        for (Invoker<T> invoker : invokers) {
            long rt = Status.getStatus(invoker.getUrl().getPort()).getElapsed();
            if (rt < minRt) {
                minRt = rt;
                result = invoker;
            }
        }
        return result;
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        Invoker<T> result = selectByThread(invokers);
        if (result == null) {
            result = selectByTps(invokers);
        }
        if (result == null) {
            result = invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
        }
        return result;
    }
}