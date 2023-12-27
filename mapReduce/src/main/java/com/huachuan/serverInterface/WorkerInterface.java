package com.huachuan.serverInterface;

public interface WorkerInterface {
    //向master注册worker服务
    int register(String address) throws Exception;

    //本地注册MapReduce服务
    void registerLocalService();


    //处理任务 通过type决定处理map还是reduce任务
    String handleTask(String type, String path);

    //处理心跳请求
    String heartBeat();
}
