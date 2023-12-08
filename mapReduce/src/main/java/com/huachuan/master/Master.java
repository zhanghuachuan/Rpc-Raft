package com.huachuan.master;

import com.huachuan.provider.RpcServer;
import com.huachuan.provider.ServerPool;
import com.huachuan.serverInterface.MasterInterface;
import com.huachuan.serverInterface.impl.MasterInterfaceImpl;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Master是负责协调任务分发的server服务，提供了对任务的管理和分发
 */
public class Master {
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            2,//核心线程池大小
            5,//获取CPU核数 System.out.println(Runtime.getRuntime().availableProcessors());
            3,//超时时间，没人调用时就会释放
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(3),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy()
    );

    private MasterInterface master = new MasterInterfaceImpl(20, 10);
    private RpcServer rpcServer;

    public void start() throws Exception {
        rpcServer = new RpcServer("127.0.0.1:8083");
        rpcServer.register(MasterInterfaceImpl.class, master, "register");
        rpcServer.start();
        //开始心跳检测
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    master.heartBeat();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        //分发任务
        while (master.getMapTask().get() > 0 || master.getReduceTask().get() > 0 || master.getOnBusy() != 0) {
            System.out.println("当前map任务数：" + master.getMapTask().get());
            System.out.println("当前reduce任务数：" + master.getReduceTask().get());
            if (master.getWorkerServerCount() <= 0) {
                System.out.println("没有worker服务可用，等待中！！！");
                Thread.sleep(5*1000);
                continue;
            }
            if (master.getMapTask().get() > 0) {
                System.out.println("分发map任务");
                threadPoolExecutor.execute(()->{master.taskDistribution("map");});
                continue;
            }
            if (master.getReduceTask().get() > 0) {
                System.out.println("分发reduce任务");
                threadPoolExecutor.execute(()->{master.taskDistribution("reduce");});
                continue;
            }
            System.out.println("map和reduce任务均已为0，等待所有忙碌worker结束");
            Thread.sleep(4*1000);
        }
        System.out.println("任务结束");
    }
}
