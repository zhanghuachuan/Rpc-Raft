package com.huachuan.master;

import com.huachuan.serverInterface.MasterInterface;
import com.huachuan.serverInterface.impl.MasterInterfaceImpl;

/**
 * Master是负责协调任务分发的server服务，提供了对任务的管理和分发
 */
public class Master {
    private MasterInterface master = new MasterInterfaceImpl();
    private static String address = "127.0.0.1:8083";
    public void start() throws Exception {
        //首先设置需要统计的文件
        master.setDir("src/main/resources/wordCountFiles/origin");
        master.start(address);
    }
}
