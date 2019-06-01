package com.aliware.tianchi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author daofeng.xjf
 * <p>
 * 服务端过滤器
 * 可选接口
 * 用户可以在服务端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = Constants.PROVIDER)
public class TestServerFilter implements Filter {
    private static final AtomicBoolean init = new AtomicBoolean(true);
    private static int activeThreads;
    private static long rtt;
    private static int threads;

    public static String getActiveCount() {
        return "#" + activeThreads + "#" + rtt + "#" + threads;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Result result;
        long begin = System.currentTimeMillis();
        URL url = URL.valueOf(invoker.getUrl().toIdentityString());
        RpcStatus.beginCount(url, invocation.getMethodName());
        RpcStatus status = RpcStatus.getStatus(url);
        if (init.get()) {
            if (init.compareAndSet(true, false)) {
                threads = Integer.valueOf(invoker.getUrl().getParameter("threads"));
            }
        }
        try {
            result = invoker.invoke(invocation);
            RpcStatus.endCount(url, invocation.getMethodName(), System.currentTimeMillis() - begin, true);
            activeThreads = status.getActive();
            rtt = status.getAverageElapsed();
        } catch (Exception e) {
            RpcStatus.endCount(url, invocation.getMethodName(), System.currentTimeMillis() - begin, false);
            activeThreads = status.getActive();
            rtt = 500;
            CallbackServiceImpl.sendCallbackImmediately();
            throw e;
        }
        return result;
    }

    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        return result;
    }

}
