package com.aliware.tianchi;

import org.apache.dubbo.rpc.listener.CallbackListener;

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
        int port = Integer.valueOf(tokens[0]);
        int thread = Integer.valueOf(tokens[1]);
        int active = Integer.valueOf(tokens[2]);
        long avgRtt = Long.valueOf(tokens[3]);
        long lastRtt = Long.valueOf(tokens[4]);
        long succeededTask = Long.valueOf(tokens[5]);
        long failedTask = Long.valueOf(tokens[5]);
        boolean catchException = Boolean.valueOf(tokens[5]);
        int index = (port - 20870) / 10;

        UserLoadBalance.threadArray[index] = thread;
        UserLoadBalance.remainderArray[index] = thread - active;
        UserLoadBalance.lastRttArray[index] = lastRtt;
        UserLoadBalance.avgRttArray[index] = avgRtt;
        UserLoadBalance.succeededTaskArray[index] = succeededTask;
        UserLoadBalance.failedTaskArray[index] = failedTask;
        UserLoadBalance.catchExceptionArray[index] = catchException;
        UserLoadBalance.activeChanged = true;
    }

}
