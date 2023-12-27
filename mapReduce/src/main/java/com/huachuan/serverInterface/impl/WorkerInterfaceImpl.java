package com.huachuan.serverInterface.impl;

import com.huachuan.consumer.RpcClient;
import com.huachuan.mapReduce.MapReduce;
import com.huachuan.serverInterface.MasterInterface;
import com.huachuan.serverInterface.WorkerInterface;
import org.apache.log4j.Logger;

public class WorkerInterfaceImpl implements WorkerInterface {
    private static Logger log = Logger.getLogger(WorkerInterfaceImpl.class);
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
    public String handleTask(String type, String path) {
        if ("map".equals(type)) {
            log.info("map被调用");
            return MapReduce.map(path);
        } else {
           log.info("reduce被调用");
            return MapReduce.reduce(path);
        }
    }

    @Override
    public String heartBeat() {
        return "ok";
    }
}
