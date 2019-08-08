package com.aliware.tianchi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

/**
 * @author daofeng.xjf
 * <p>
 * 服务端过滤器
 * 可选接口
 * 用户可以在服务端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = Constants.PROVIDER)
public class TestServerFilter implements Filter {

    private static int thread = -1;
    static int current = 0;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Result result;
        // long begin = System.currentTimeMillis();
        try {
            result = invoker.invoke(invocation);
        } catch (Exception e) {
            throw e;
        }
        if (thread == -1) {
            thread = Integer.parseInt(invoker.getUrl().getParameter("threads"));
        }
        result.setAttachment("active", String.valueOf(thread - current));
        // result.setAttachment("rt", String.valueOf(System.currentTimeMillis() - begin));
        return result;
    }

    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        return result;
    }

}