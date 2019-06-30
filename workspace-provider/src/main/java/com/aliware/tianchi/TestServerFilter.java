package com.aliware.tianchi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author daofeng.xjf
 * <p>
 * 服务端过滤器
 * 可选接口
 * 用户可以在服务端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = Constants.PROVIDER)
public class TestServerFilter implements Filter {
    private static Status status = null;
    private static int threads = 0;
    private static int port = 0;
    private Timer timer = new Timer();
    private Timer change = new Timer();

    private static int activeThreads;

    private static long tps;
    private static long lastRtt = 1000;
    // private static long succeededTask = 0;
    // private static long failedTask = 0;

    // private static boolean catchException = false;

    public static ConcurrentLinkedDeque<Long> rttTimeout = new ConcurrentLinkedDeque<>();

    private static void initPort() {
        if (port == 0) {
            String quota = System.getProperty("quota");
            if ("small".equalsIgnoreCase(quota)) {
                port = 20880;
            } else if ("medium".equalsIgnoreCase(quota)) {
                port = 20870;
            } else if ("large".equalsIgnoreCase(quota)) {
                port = 20890;
            }
        }
    }

    static String getActiveCount() {
        initPort();
        StringBuilder builder = new StringBuilder();
        builder.append(port);
        builder.append("#");
        builder.append(threads);
        builder.append("#");
        builder.append(activeThreads);
        builder.append("#");
        builder.append(tps);
        builder.append("#");
        builder.append(lastRtt);
        // builder.append("#");
        // builder.append(succeededTask);
        // builder.append("#");
        // builder.append(failedTask);
        // builder.append("#");
        // builder.append(catchException);
        return builder.toString();
    }

    static int getThreads() {
        return threads;
    }


    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        URL url = invoker.getUrl();
        if (status == null) {
            synchronized (TestServerFilter.class) {
                if (status == null) {
                    initPort();
                    status = Status.getStatus(url);
                    threads = Integer.valueOf(url.getParameter("threads"));
                    port = url.getPort();
                    // timer.schedule(new TimerTask() {
                    //
                    //     @Override
                    //     public void run() {
                    //         long now = System.currentTimeMillis();
                    //         if (lastRtt > status.getSucceededMaxElapsed() * 10) {
                    //             rttTimeout.addLast(now);
                    //         }
                    //         while (!rttTimeout.isEmpty()) {
                    //             long last = rttTimeout.getFirst();
                    //             if (now - last > 5) rttTimeout.removeFirst();
                    //             else break;
                    //         }
                    //         // System.out.println(rttTimeout.size());
                    //     }
                    // }, 0, 1);
                    change.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Status.resetElapsed(url);
                        }
                    }, 6000, 6000);
                }
            }
        }
        Result result;
        long begin = System.currentTimeMillis();
        Status.beginCount(url);
        try {
            result = invoker.invoke(invocation);
            lastRtt = System.currentTimeMillis() - begin;
            Status.endCount(url, lastRtt, true);
            // catchException = false;
        } catch (Exception e) {
            lastRtt = System.currentTimeMillis() - begin;
            Status.endCount(url, lastRtt, false);
            // catchException = true;
            throw e;
        }
        activeThreads = status.getActive();
        tps = status.getAverageTps();
        // succeededTask = status.getSucceeded();
        // failedTask = status.getFailed();
        // if (catchException) CallbackServiceImpl.sendCallbackImmediately();
        return result;
    }

    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        return result;
    }

}
