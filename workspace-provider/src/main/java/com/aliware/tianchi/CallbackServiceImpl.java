package com.aliware.tianchi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.common.store.DataStore;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.listener.CallbackListener;
import org.apache.dubbo.rpc.service.CallbackService;

import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author daofeng.xjf
 * <p>
 * 服务端回调服务
 * 可选接口
 * 用户可以基于此服务，实现服务端向客户端动态推送的功能
 */
public class CallbackServiceImpl implements CallbackService {

    static final Map<String, CallbackListener> listeners = new ConcurrentHashMap<>();

    static String maxThreads(String statusMessage, String mark) {
        int maxIndex = statusMessage.indexOf(mark);
        int commaIndex = statusMessage.indexOf(",", maxIndex);
        return statusMessage.substring(maxIndex + mark.length(), commaIndex);
    }

    @Override
    public void addListener(String key, CallbackListener listener) {
        listeners.put(key, listener);
        DataStore dataStore = ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();
        Map<String, Object> executors = dataStore.get(Constants.EXECUTOR_SERVICE_COMPONENT_KEY);
        for (Map.Entry<String, Object> entry : executors.entrySet()) {
            ExecutorService executor = (ExecutorService) entry.getValue();
            ThreadPoolExecutor tp = (ThreadPoolExecutor) executor;
            listener.receiveServerMsg(String.format("%d,%d", tp.getMaximumPoolSize(), RpcContext.getContext().getUrl().getPort()));
        }
    }
}