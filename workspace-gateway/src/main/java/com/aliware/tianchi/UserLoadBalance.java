package com.aliware.tianchi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;
import java.util.concurrent.*;

/**
 * @author daofeng.xjf
 * <p>
 * 负载均衡扩展接口
 * 必选接口，核心接口
 * 此类可以修改实现，不可以移动类或者修改包名
 * 选手需要基于此类实现自己的负载均衡算法
 */
public class UserLoadBalance implements LoadBalance {


    private static final ScheduledExecutorService EXECUTOR = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("Thread Selector"));

    private static boolean initExecutor = true;
    private static boolean checkByThread = true;
    private static boolean initFinish = false;


    private <T> Invoker<T> selectByThread(List<Invoker<T>> invokers) {
        if (initExecutor) {
            synchronized (UserLoadBalance.class) {
                if (initExecutor) {
                    EXECUTOR.scheduleAtFixedRate(() -> {
                        checkByThread = true;
                        initFinish = true;
                        Status.refreshCurrent();
                    }, 0, 100, TimeUnit.MILLISECONDS);
                }
                initExecutor = false;
            }
        }
        int sum = Status.getCurrent();
        if (sum > 0) {
            int offset = ThreadLocalRandom.current().nextInt(sum);
            for (Invoker<T> invoker : invokers) {
                offset -= Status.getStatus(invoker.getUrl().getPort()).getCanUseRemainder();
                if (offset < 0) {
                    return invoker;
                }
            }
        }
        if (initFinish && sum <= 0) {
            checkByThread = false;
        }
        return null;
    }

    private <T> Invoker<T> selectByRt(List<Invoker<T>> invokers) {
        double sum = 10000 / Status.getTotalAvgElapsed() * 3;
        if (sum > 0) {
            double offset = ThreadLocalRandom.current().nextDouble(sum);
            for (Invoker<T> invoker : invokers) {
                Status status = Status.getStatus(invoker.getUrl().getPort());
                long lastRt = status.getLastElapsed();
                if (!checkByThread && lastRt > Status.getTotalAvgElapsed() * 3) {
                    continue;
                }
                double rt = status.getAvgElapsed();
                offset -= 10000 / rt;
                if (offset < 0 && lastRt < rt * 3) {
                    return invoker;
                }
            }
        }
        return null;
    }

    private <T> Invoker<T> selectByRemainder(List<Invoker<T>> invokers) {
        int maxThread = 0;
        Invoker<T> result = invokers.get(0);
        for (Invoker<T> invoker : invokers) {
            int r = Status.getStatus(invoker.getUrl().getPort()).getRemainder();
            if (r > maxThread) {
                result = invoker;
                maxThread = r;
            }
        }
        return result;
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        Invoker<T> result = null;
        while (result == null) {
            if (checkByThread) {
                result = selectByThread(invokers);
                if (result != null) return result;
            }
            result = selectByRt(invokers);
        }
        return result;
        // return result != null ? result : selectByRemainder(invokers);
    }
}