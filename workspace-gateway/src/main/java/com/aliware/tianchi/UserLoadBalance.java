package com.aliware.tianchi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;
import java.util.concurrent.*;

/**
 * @author daofeng.xjf
 * <p>
 * 负载均衡扩展接口
 * 必选接口，核心接口
 * 此类可以修改实现，不可以移动类或者修改包名
 * 选手需要基于此类实现自己的负载均衡算法
 */
public class UserLoadBalance implements LoadBalance {


    static ConcurrentLinkedDeque<ProviderThread> queue = new ConcurrentLinkedDeque<>();
    static double avg = 1;

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        Invoker<T> result = null;
        ProviderThread request;
        do {
            while ((request = queue.poll()) == null) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (request.rtt < UserLoadBalance.avg) {
                break;
            } else {
                request.rtt = (request.rtt / 2);
                queue.add(request);
            }
        } while (true);
        for (Invoker<T> tInvoker : invokers) {
            if (tInvoker.getUrl().getPort() == request.port) {
                result = tInvoker;
                break;
            }
        }
        return result;
    }
}