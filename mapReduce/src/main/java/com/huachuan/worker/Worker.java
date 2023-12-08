package com.huachuan.worker;

import com.huachuan.provider.RpcServer;
import com.huachuan.serverInterface.WorkerInterface;
import com.huachuan.serverInterface.impl.WorkerInterfaceImpl;

public class Worker {
    private WorkerInterface worker = new WorkerInterfaceImpl();
    private RpcServer rpcServer;
    private String address;
    public Worker(String address) {
        this.address = address;
    }

    public void start() throws Exception {
        rpcServer = new RpcServer(address);
        rpcServer.register(WorkerInterfaceImpl.class, worker, "handleTask");
        rpcServer.register(WorkerInterfaceImpl.class, worker, "heartBeat");
        rpcServer.start();
        worker.register(address);
        System.in.read();
    }


}
