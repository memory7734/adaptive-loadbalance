package com.aliware.tianchi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.store.DataStore;
import org.apache.dubbo.rpc.*;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author daofeng.xjf
 * <p>
 * 服务端过滤器
 * 可选接口
 * 用户可以在服务端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = Constants.PROVIDER)
public class TestServerFilter implements Filter {


    private static int count = 0;
    private static ThreadPoolExecutor tp;


    private void init() {
        DataStore dataStore = ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();
        Map<String, Object> executors = dataStore.get(Constants.EXECUTOR_SERVICE_COMPONENT_KEY);
        for (Map.Entry<String, Object> entry : executors.entrySet()) {
            ExecutorService executor = (ExecutorService) entry.getValue();
            if (executor instanceof ThreadPoolExecutor) {
                if (tp == null) {
                    tp = (ThreadPoolExecutor) executor;
                }
                break;
            }
        }
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (tp == null) {
            init();
        }
        Result result;
        long begin = System.currentTimeMillis();
        try {

            result = invoker.invoke(invocation);
        } catch (Exception e) {
            throw e;
        }
        result.setAttachment("rt", String.valueOf(System.currentTimeMillis() - begin));
        result.setAttachment("active", String.valueOf(tp.getActiveCount()));
        if (count < 5) {
            result.setAttachment("thread", String.valueOf(tp.getCorePoolSize()));
            count++;
        }
        return result;
    }

    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        return result;
    }

}