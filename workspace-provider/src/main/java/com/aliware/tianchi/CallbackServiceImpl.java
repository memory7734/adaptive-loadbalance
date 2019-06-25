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
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!listeners.isEmpty()) {
                    int port = TestServerFilter.getPort();
                    if (port == 0) {
                        String quota = System.getProperty("quota");
                        if ("small".equalsIgnoreCase(quota)){
                            port = 20880;
                        } else if ("medium".equalsIgnoreCase(quota)) {
                            port = 20870;
                        } else if ("large".equalsIgnoreCase(quota)) {
                            port = 20890;
                        }
                        TestServerFilter.setPort(port);
                    }
                    for (Map.Entry<String, CallbackListener> entry : listeners.entrySet()) {
                        try {
                            entry.getValue().receiveServerMsg(port + TestServerFilter.getActiveCount());
                        } catch (Throwable t1) {
                            listeners.remove(entry.getKey());
                        }
                    }
                }
            }
        }, 0, 1);
    }

    static void sendCallbackImmediately() {
        int port = TestServerFilter.getPort();
        if (port == 0) {
            String quota = System.getProperty("quota");
            if ("small".equalsIgnoreCase(quota)){
                port = 20880;
            } else if ("medium".equalsIgnoreCase(quota)) {
                port = 20870;
            } else if ("large".equalsIgnoreCase(quota)) {
                port = 20890;
            }
            TestServerFilter.setPort(port);
        }
        for (Map.Entry<String, CallbackListener> entry : listeners.entrySet()) {
            try {
                entry.getValue().receiveServerMsg(port + TestServerFilter.getActiveCount());
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
        listener.receiveServerMsg(new Date().toString()); // send notification for change
    }
}
