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
        if (tokens.length < 2) return;
        String host = tokens[0];
        int thread = Integer.valueOf(tokens[1]);
        if (UserLoadBalance.threadMap.get(host) == null) {
            UserLoadBalance.threadMap.put(host, thread);
            UserLoadBalance.threadChanged.set(true);
        }
    }

}
