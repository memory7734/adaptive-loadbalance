package com.aliware.tianchi;

import org.apache.dubbo.rpc.listener.CallbackListener;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author daofeng.xjf
 * <p>
 * 客户端监听器
 * 可选接口
 * 用户可以基于获取获取服务端的推送信息，与 CallbackService 搭配使用
 */
public class CallbackListenerImpl implements CallbackListener {


    @Override
    public void receiveServerMsg(String msg) {
        String[] tokens = msg.split("\\|");
        long totalThreads = 0;
        for (int i = 1; i < tokens.length; i++) {
            String[] kv = tokens[i].split("#");
            String url = kv[0];
            int active = Integer.valueOf(kv[1]);
            long rtt = Long.valueOf(kv[2]);
            long thread = Long.valueOf(kv[3]);
            AtomicInteger integer = UserLoadBalance.activeMap.get(url);
            if (integer == null) {
                UserLoadBalance.activeMap.putIfAbsent(url, new AtomicInteger());
                integer = UserLoadBalance.activeMap.get(url);
            }
            integer.set(active);
            UserLoadBalance.rttMap.put(url, rtt);
            UserLoadBalance.threadMap.put(url, thread);
            totalThreads += thread;
        }
        for (Map.Entry<String, Long> entry : UserLoadBalance.threadMap.entrySet()) {
            UserLoadBalance.performanceMap.put(entry.getKey(), 1.0 * totalThreads / entry.getValue());
        }
    }

}
