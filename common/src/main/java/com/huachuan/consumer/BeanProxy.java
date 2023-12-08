package com.huachuan.consumer;

import com.huachuan.entity.ReturnInfo;
import com.huachuan.entity.TransInfo;
import com.huachuan.protocal.Netty.client.NettyClientImpl;
import com.huachuan.protocal.Netty.client.ZookeeperSubscribeImpl;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class BeanProxy {
    /**
     * 采用zookeeper作为注册中心自动获取远程服务地址
     * @param serivceClass 需要代理的接口
     * @param className    需要调用远端服务的实现类的类名
     * @return
     */
    public static Object getBean(final Class<?> serivceClass, String className) {

        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{serivceClass}, (proxy, method, args) -> {
                    //根据方法名获取远端服务
                    List<String> serverList = ZookeeperSubscribeImpl.getServerList(className + "." + method.getName());
                    if(serverList == null) {
                        Thread.sleep(1000);
                        serverList = ZookeeperSubscribeImpl.getServerList(className + "." + method.getName());
                        if(serverList == null) return null;
                    }
                    if (serverList.isEmpty()) return null;
                    //负载均衡
                    NettyClientImpl nettyClient = ClientPool.get(serverList.get(0));
                    if (nettyClient == null) return null;
                    TransInfo transInfo = new TransInfo();
                    transInfo.className = className;
                    transInfo.methodName = method.getName();
                    transInfo.parameterTypes = new ArrayList<>();
                    transInfo.parameters = new ArrayList<>();
                    for (int i = 0; i < args.length; ++i) {
                        transInfo.parameterTypes.add(args[i].getClass());
                        transInfo.parameters.add(args[i]);
                    }

                    ReturnInfo result = nettyClient.send(transInfo);

                    return result.value;
                });

    }
}