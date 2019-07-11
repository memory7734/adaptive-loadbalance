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
 *
 * 负载均衡扩展接口
 * 必选接口，核心接口
 * 此类可以修改实现，不可以移动类或者修改包名
 * 选手需要基于此类实现自己的负载均衡算法
 */
public class UserLoadBalance implements LoadBalance {

    static int total = 0;

    private static final ScheduledExecutorService EXECUTOR = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("Thread Selector"));

    private static boolean initExecutor = true;
    private static boolean checkByThread = true;

    static void calcTotal() {

        total = (Status.getStatus(20870).getCanUseRemainder()
                + Status.getStatus(20880).getCanUseRemainder()
                + Status.getStatus(20890).getCanUseRemainder());

    }

    private <T> Invoker<T> selectByThread(List<Invoker<T>> invokers) {
        if (initExecutor) {
            synchronized (UserLoadBalance.class) {
                if (initExecutor) {
                    EXECUTOR.scheduleAtFixedRate(() -> {
                        checkByThread = true;
                    }, 10000, 50, TimeUnit.MILLISECONDS);
                }
                initExecutor = false;
            }
        }
        int sum = total;
        if (sum > 0) {
            int offset = ThreadLocalRandom.current().nextInt(sum);
            for (Invoker<T> invoker : invokers) {
                offset -= Status.getStatus(invoker.getUrl().getPort()).getCanUseRemainder();
                if (offset < 0) {
                    return invoker;
                }
            }
        }
        if (!initExecutor){
            checkByThread = false;
        }
        return null;
    }

    private <T> Invoker<T> selectByTps(List<Invoker<T>> invokers) {
        Invoker<T> result = null;
        long minRt = Long.MAX_VALUE;
        for (Invoker<T> invoker : invokers) {
            long rt = Status.getStatus(invoker.getUrl().getPort()).getAvgElapsed();
            if (rt < minRt) {
                minRt = rt;
                result = invoker;
            }
        }
        return result;
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        Invoker<T> result = null;
        if (checkByThread) {
            result = selectByThread(invokers);
        }
        if (result == null) {
            System.out.println("select by thread");
            result = selectByTps(invokers);
        }
        if (result == null) {
            System.out.println("select by rt");
            result = invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
            System.out.println("select by random");
        }
        return result;
    }
}