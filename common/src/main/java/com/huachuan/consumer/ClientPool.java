package com.huachuan.consumer;

import com.huachuan.protocal.Netty.client.NettyClientImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientPool {
    //用于存放ip+port与客户端连接的映射
    private static Map<String, NettyClientImpl> map = new ConcurrentHashMap();

    public static NettyClientImpl get(String address) {
        if(!map.containsKey(address)) {
            System.out.println("创建客户端，连接：" + address);
            String[] component = address.split(":");
            new NettyClientImpl(component[0], Integer.valueOf(component[1]));
        }
        return map.get(address);
    }

    public static void add(String address, NettyClientImpl client) {
        if(map.containsKey(address)) return;
        map.put(address, client);
    }

    public static void remove(String address) {
        System.out.println("移除客户端:" + address);
        if(map.containsKey(address)) {
            map.get(address).closeClient();
            map.remove(address);
        }
    }

}
