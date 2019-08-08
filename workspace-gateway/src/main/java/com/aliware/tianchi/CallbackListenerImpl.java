package com.aliware.tianchi;

import org.apache.dubbo.rpc.listener.CallbackListener;

/**
 * @author daofeng.xjf
 *
 * 客户端监听器
 * 可选接口
 * 用户可以基于获取获取服务端的推送信息，与 CallbackService 搭配使用
 *
 */
public class CallbackListenerImpl implements CallbackListener {

    @Override
    public void receiveServerMsg(String msg) {
        String[] s = msg.split(",");
        int port = Integer.parseInt(s[1]);
        int max = Integer.parseInt(s[0]);
        ProviderStatus provider = new ProviderStatus(port, max);
        ProviderStatus.providers[(port - 20870) / 10] = provider;
        provider.init();
    }

}