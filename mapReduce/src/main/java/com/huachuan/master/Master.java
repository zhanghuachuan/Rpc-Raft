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
    ThreadPoolExecutor threadPoolExecutor;

    private MasterInterface master = new MasterInterfaceImpl(100000, 50000);
    private RpcServer rpcServer;

    public void start() throws Exception {
        rpcServer = new RpcServer("127.0.0.1:8083");
        rpcServer.register(MasterInterfaceImpl.class, master, "register");
       threadPoolExecutor = new ThreadPoolExecutor(
                2,//核心线程池大小
                5,//获取CPU核数 System.out.println(Runtime.getRuntime().availableProcessors());
                3,//超时时间，没人调用时就会释放
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(master.getMapTask() + master.getReduceTask()) ,
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
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
        }).start();

        //分发任务
        while (master.getMapTask() > 0 || master.getReduceTask() > 0 || master.getOnBusy() != 0) {
            if (master.getMapTask() > 0) {
                System.out.println("启动线程分发map任务");
                for (int i = 0; i < master.getMapTask(); ++i) {
                    threadPoolExecutor.execute(()->{master.taskDistribution("map");});
                }
            } else  {
                if (master.getReduceTask() > 0) {
                    System.out.println("启动线程分发reduce任务");
                    for (int i = 0; i < master.getReduceTask(); ++i) {
                        threadPoolExecutor.execute(()->{master.taskDistribution("reduce");});
                    }
                }
            }


            //等待线程池任务执行完毕
           while (threadPoolExecutor.getCompletedTaskCount() != threadPoolExecutor.getTaskCount()){
               System.out.println("线程池中已完成任务个数：" + threadPoolExecutor.getCompletedTaskCount());
               System.out.println("线程池线程仍在执行中！！！！");
               Thread.sleep(1*1000);
           };

        }

        System.out.println("任务结束");
        System.in.read();
    }
}
