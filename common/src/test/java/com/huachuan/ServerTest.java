package com.huachuan;

import com.huachuan.messages.impl.Echo;
import com.huachuan.provider.RpcServer;
import com.huachuan.provider.ServerPool;
import org.junit.Test;

public class ServerTest {
    @Test
    public void server() throws Exception {
        RpcServer rpcServer1 = ServerPool.get("127.0.0.1:8083");
        rpcServer1.register(Echo.class, new Echo(), "echo");
        rpcServer1.start();
        RpcServer rpcServer2 = ServerPool.get("127.0.0.1:8084");
        rpcServer2.register(Echo.class, new Echo(), "echo");
        rpcServer2.start();
        System.in.read();
    }
}
