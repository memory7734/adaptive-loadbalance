package com.aliware.tianchi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author daofeng.xjf
 * <p>
 * 客户端过滤器
 * 可选接口
 * 用户可以在客户端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = Constants.CONSUMER)
public class TestClientFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            return invoker.invoke(invocation);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        int index = (invoker.getUrl().getPort() - 20870) / 10;
        UserLoadBalance.remainder[index] = Integer.parseInt(result.getAttachment("active"));
        int rttIndex = ThreadLocalRandom.current().nextInt(1024);
        long lastRtt = UserLoadBalance.rtt[index][rttIndex];
        long curRtt = Integer.parseInt(result.getAttachment("rtt"));
        UserLoadBalance.avgRtt[index] = UserLoadBalance.avgRtt[index] * 1024 - lastRtt + curRtt;
        UserLoadBalance.rtt[index][rttIndex] = curRtt;
        return result;
    }
}