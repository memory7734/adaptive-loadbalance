package com.aliware.tianchi;

import org.apache.dubbo.rpc.listener.CallbackListener;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
        String[] tokens = msg.split("#");
        if (tokens.length < 4) return;
        String host = tokens[0];
        int active = Integer.valueOf(tokens[1]);
        long rtt = Long.valueOf(tokens[2]);
        long thread = Long.valueOf(tokens[3]);
        AtomicInteger integer = UserLoadBalance.activeMap.get(host);
        if (integer == null) {
            UserLoadBalance.activeMap.putIfAbsent(host, new AtomicInteger());
            integer = UserLoadBalance.activeMap.get(host);
        }
        integer.set(active);
        UserLoadBalance.rttMap.put(host, rtt);
        if (UserLoadBalance.threadMap.get(host) == null) {
            UserLoadBalance.threadMap.put(host, thread);
            UserLoadBalance.threadChanged.set(true);
        }
    }

}
