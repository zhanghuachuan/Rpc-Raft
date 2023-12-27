package com.huachuan.serverInterface;

public interface MasterInterface {

    //获取正在被处理的任务个数
    int getOnBusy();

    //提供远程worker注册服务
    int register(String address);

    //获取worker服务端的个数
    int getWorkerServerCount();

    //任务分发(worker请求任务， master为其分发任务)
    void taskDistribution(String type, String path);

    //监测worker状态，心跳检测
    void heartBeat() throws InterruptedException;

    void start(String address) throws Exception;

    //设置需要被处理的文件夹
    void setDir(String dir);
}
