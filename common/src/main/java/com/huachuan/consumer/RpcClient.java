package com.huachuan.consumer;

import com.huachuan.entity.ReturnInfo;
import com.huachuan.entity.TransInfo;
import com.huachuan.protocal.Netty.client.NettyClientImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * client 未使用远程zookeeper来作为注册中心
 */
public class RpcClient {
    private String address;
    NettyClientImpl nettyClient;

    public RpcClient(String address) {
        this.address = address;
        String[] component = address.split(":");
        nettyClient = new NettyClientImpl(component[0], Integer.valueOf(component[1]));
    }


    public Object request(final Class<?> serivceClass, String className, String methodName, Object...args) throws Exception {
        TransInfo transInfo = new TransInfo();
        transInfo.className = className;
        transInfo.methodName = methodName;
        transInfo.parameterTypes = new ArrayList<>();
        transInfo.parameters = new ArrayList<>();
        for (int i = 0; i < args.length; ++i) {
            transInfo.parameterTypes.add(args[i].getClass());
            transInfo.parameters.add(args[i]);
        }

        ReturnInfo result = nettyClient.send(transInfo);
        return result.value;
    }

    public void closeClient() {
        nettyClient.closeClient();
    }

    public int getStatus() {
        return nettyClient.getStatus();
    }

}