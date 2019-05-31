package com.aliware.tianchi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author daofeng.xjf
 * <p>
 * 服务端过滤器
 * 可选接口
 * 用户可以在服务端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = Constants.PROVIDER)
public class TestServerFilter implements Filter {
    private static final ConcurrentHashMap<String, Integer> activeCount = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> rttMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> threadMap = new ConcurrentHashMap<>();

    public static String getActiveCount() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : activeCount.entrySet()) {
            builder.append("|");
            builder.append(entry.getKey());
            builder.append("#");
            builder.append(entry.getValue());
            builder.append("#");
            builder.append(rttMap.getOrDefault(entry.getKey(), 1000L));
            builder.append("#");
            builder.append(threadMap.getOrDefault(entry.getKey(), 200L));
        }
        return builder.toString();
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Result result;
        long begin = System.currentTimeMillis();
        URL url = URL.valueOf(invoker.getUrl().toIdentityString());
        RpcStatus.beginCount(url, invocation.getMethodName());
        RpcStatus status = RpcStatus.getStatus(url);
        threadMap.put(url.toString(), Long.valueOf(Integer.valueOf(invoker.getUrl().getParameter("threads"))));
        try {
            result = invoker.invoke(invocation);
            RpcStatus.endCount(url, invocation.getMethodName(), System.currentTimeMillis() - begin, true);
            activeCount.put(url.toString(), status.getActive());
            rttMap.put(url.toString(), status.getAverageElapsed());
        } catch (Exception e) {
            RpcStatus.endCount(url, invocation.getMethodName(), System.currentTimeMillis() - begin, false);
            activeCount.put(url.toString(), status.getActive());
            rttMap.put(url.toString(), 1000L);
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
