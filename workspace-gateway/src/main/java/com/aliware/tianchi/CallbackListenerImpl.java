package com.aliware.tianchi;

import org.apache.dubbo.rpc.listener.CallbackListener;

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
        int port = Integer.valueOf(tokens[0]);
        int thread = Integer.valueOf(tokens[1]) * 4 / 5;
        int active = Integer.valueOf(tokens[2]);
        long rtt = Long.valueOf(tokens[3]);
        long lastRtt = Long.valueOf(tokens[4]);
        if (port == 0) return;
        int index = (port - 20870) / 10;
        UserLoadBalance.averageRttArray[index] = rtt;
        UserLoadBalance.lastRttArray[index] = lastRtt;
        UserLoadBalance.threadArray[index] = thread;
        for (; ; ) {
            if (UserLoadBalance.activeChanged.compareAndSet(false, true)) {
                UserLoadBalance.activeArray[index] = active;
                break;
            }
        }
    }

}
