package com.huachuan.provider;

import com.huachuan.protocal.Netty.client.NettyClientImpl;
import com.huachuan.protocal.Netty.server.NettyServerImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerPool {
    //用于存放ip+port与客户端连接的映射
    private static Map<String, RpcServer> map = new ConcurrentHashMap();

    public synchronized static RpcServer get(String address) {
        if(!map.containsKey(address)) {
            System.out.println("开启服务端" + address);
            map.put(address, new RpcServer(address));
        }
        return map.get(address);
    }

    public synchronized static void add(String address, RpcServer server) {
        if(map.containsKey(address)) return;
        map.put(address, server);
    }

    public synchronized static void remove(String address) {
        if(map.containsKey(address)) {
            map.get(address).closeServer();
            map.remove(address);
        }
    }

}
