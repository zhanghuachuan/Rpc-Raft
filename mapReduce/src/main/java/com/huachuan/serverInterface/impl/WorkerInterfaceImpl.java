package com.huachuan.serverInterface.impl;

import com.huachuan.consumer.RpcClient;
import com.huachuan.serverInterface.MasterInterface;
import com.huachuan.serverInterface.WorkerInterface;

public class WorkerInterfaceImpl implements WorkerInterface {
    public static String MASTER_ARRESS = "127.0.0.1:8083";
    private RpcClient client = new RpcClient(MASTER_ARRESS);

    @Override
    public int register(String address) throws Exception {
        if (client == null) return 0;
        int res = (int)client.request(MasterInterface.class, "MasterInterfaceImpl", "register", address);
        return res;
    }

    @Override
    public void registerLocalService() {

    }


    @Override
    public int handleTask(String type) {
        if ("map".equals(type)) {
            System.out.println("map被调用");
        } else {
            System.out.println("reduce被调用");
        }
        try {
            Thread.sleep(2 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 1;
    }

    @Override
    public String heartBeat() {
        return "ok";
    }
}
