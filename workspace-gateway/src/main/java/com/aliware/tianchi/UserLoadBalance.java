package com.aliware.tianchi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
    // static int[] threadArray = new int[3];
    // static int[] remainderArray = new int[3];
    // static long[] tpsArray = new long[3];
    // static long[] weightArray = new long[3];
    // static long[] lastRttArray = new long[3];
    // static long[] succeededTaskArray = new long[3];
    // static long[] failedTaskArray = new long[3];
    // static boolean[] catchExceptionArray = new boolean[3];
    // static long[] requestLimitTime = new long[3];

    // static boolean activeChanged = false;

    static AtomicInteger total = new AtomicInteger();

    private Timer timer = new Timer();

    public static void calcTotal() {

        total.set(Status.getStatus(20870).getRemainder()
                + Status.getStatus(20880).getRemainder()
                + Status.getStatus(20890).getRemainder());

    }

    UserLoadBalance() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                calcTotal();
            }
        }, 0, 500);
    }

    private <T> Invoker<T> selectByThread(List<Invoker<T>> invokers) {

        if (total.get() > 0) {
            int offset = ThreadLocalRandom.current().nextInt(total.get());

            for (Invoker<T> invoker : invokers) {
                offset -= Status.getStatus(invoker.getUrl().getPort()).getRemainder();
                if (offset < 0) return invoker;
            }
        }
        return null;
    }

    // private <T> Invoker<T> selectByTps(List<Invoker<T>> invokers) {
    //     long sum = 0;
    //     for (long x : weightArray) {
    //         sum += x;
    //     }
    //     if (sum > 0) {
    //         long offset = ThreadLocalRandom.current().nextLong(sum);
    //         for (Invoker<T> invoker : invokers) {
    //             int index = (invoker.getUrl().getPort() - 20870) / 10;
    //             offset -= weightArray[index];
    //             if (offset < 0) return invoker;
    //         }
    //     }
    //     return null;
    // }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        Invoker<T> result = selectByThread(invokers);
        // if (result == null) {
        //     result = selectByTps(invokers);
        // }
        if (result == null) {
            result = invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
        }
        return result;
    }
}