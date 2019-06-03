package com.aliware.tianchi;

import org.apache.dubbo.rpc.listener.CallbackListener;
import org.apache.dubbo.rpc.service.CallbackService;

import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author daofeng.xjf
 * <p>
 * 服务端回调服务
 * 可选接口
 * 用户可以基于此服务，实现服务端向客户端动态推送的功能
 */
public class CallbackServiceImpl implements CallbackService {

    public CallbackServiceImpl() {
        String host = "provider-" + System.getProperty("quota");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!listeners.isEmpty()) {
                    for (Map.Entry<String, CallbackListener> entry : listeners.entrySet()) {
                        try {
                            entry.getValue().receiveServerMsg(host + TestServerFilter.getActiveCount());
                        } catch (Throwable t1) {
                            listeners.remove(entry.getKey());
                        }
                    }
                }
            }
        }, 0, 10);
    }

    static void sendCallbackImmediately() {
        String host = "provider-" + System.getProperty("quota");
        for (Map.Entry<String, CallbackListener> entry : listeners.entrySet()) {
            try {
                entry.getValue().receiveServerMsg(host + TestServerFilter.getActiveCount());
            } catch (Throwable t1) {
                listeners.remove(entry.getKey());
            }
        }
    }

    private Timer timer = new Timer();

    /**
     * key: listener type
     * value: callback listener
     */
    private static final Map<String, CallbackListener> listeners = new ConcurrentHashMap<>();

    @Override
    public void addListener(String key, CallbackListener listener) {
        listeners.put(key, listener);
        listener.receiveServerMsg("provider-" + System.getProperty("quota") + TestServerFilter.getActiveCount()); // send notification for change
    }
}
