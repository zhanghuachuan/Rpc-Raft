package com.huachuan.serverInterface.impl;

import com.huachuan.consumer.RpcClient;
import com.huachuan.provider.RpcServer;
import com.huachuan.serverInterface.MasterInterface;
import com.huachuan.serverInterface.WorkerInterface;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MasterInterfaceImpl implements MasterInterface {
    private static Logger log = Logger.getLogger(MasterInterfaceImpl.class);
    final Lock lock = new ReentrantLock();//锁对象
    final Condition empty  = lock.newCondition();//写线程条件变量
    private RpcServer rpcServer;
    ThreadPoolExecutor threadPoolExecutor;

    //存储所有的worker
    Map<String, RpcClient> workerMap = new ConcurrentHashMap<>();
    //空闲worker的ip地址
    List<String> avaliableWorkers = Collections.synchronizedList(new ArrayList<>());
    //所有worker的ip地址
    List<String> workers = Collections.synchronizedList(new ArrayList<>());

    public List<String> originPaths = Collections.synchronizedList(new ArrayList<>());
    public List<String> mapedPaths = Collections.synchronizedList(new ArrayList<>());
    public List<String> reducedPaths = Collections.synchronizedList(new ArrayList<>());



    @Override
    public int getOnBusy() {
        return workers.size() - avaliableWorkers.size();
    }

    public  int register(String address) {
        if (workerMap.containsKey(address)) return 0;
        workerMap.put(address, new RpcClient(address));
        lock.lock();
        avaliableWorkers.add(address);
        empty.signalAll();
        lock.unlock();
        workers.add(address);
        return 1;
    }

    @Override
    public synchronized int getWorkerServerCount() {
        return avaliableWorkers.size();
    }

    public void taskDistribution(String type, String path){
        String url = "";
        lock.lock();
        try {
            while (avaliableWorkers.size() <= 0) {
                empty.await();
            }
            url = avaliableWorkers.get(avaliableWorkers.size() - 1);
            avaliableWorkers.remove(avaliableWorkers.size() - 1);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

      try{
        String result = (String)workerMap.get(url).request(WorkerInterface.class, "WorkerInterfaceImpl", "handleTask", type, path);
        if(result == null) {
            if ("map".equals(type)) originPaths.add(path);
            else mapedPaths.add(path);
        } else {
            if ("map".equals(type)) mapedPaths.add(result);
            else reducedPaths.add(result);
        }
      } catch (Exception e) { //远程worker处理失败， 任务需要重新被执行
          log.warn("worker任务失败", e);
          if ("map".equals(type)) originPaths.add(path);
          else mapedPaths.add(path);
        } finally { //无论处理成功还是失败，都需要将其加入空闲列表，去除无效连接让heartBeat
          lock.lock();
          avaliableWorkers.add(url);
          empty.signalAll();
          lock.unlock();
      }
    }

    //每5s对所有的服务提供者检查一次
    public void heartBeat() throws InterruptedException {
        while (true) {
            for(String url : workers) {
                try {
                    String res = (String) workerMap.get(url).request(WorkerInterface.class, "WorkerInterfaceImpl", "heartBeat");
                   log.info("收到来自" + url + "的心跳信息：" + res);
                } catch (Exception e) {
                    workers.remove(url);
                    avaliableWorkers.remove(url);
                    workerMap.get(url).closeClient();
                    workerMap.remove(url);
                    log.info(url + "断开连接");
                }
            }
            Thread.sleep(5*1000);
        }
    }



    public void start(String address) throws Exception {
        rpcServer = new RpcServer(address);
        rpcServer.register(MasterInterfaceImpl.class, this, "register");
        threadPoolExecutor = new ThreadPoolExecutor(
                3,//核心线程池大小 最好与worker服务个数保持一致
                5,//获取CPU核数 System.out.println(Runtime.getRuntime().availableProcessors());
                3,//超时时间，没人调用时就会释放
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(originPaths.size() * 2 + 2) ,
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        rpcServer.start();
        //开始心跳检测
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                   heartBeat();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //分发任务
        while (originPaths.size() != 0 || mapedPaths.size() != 0 || getOnBusy() != 0) {
            if (originPaths.size() > 0) {
                log.info("启动线程分发map任务");
                int n = originPaths.size();
                for (int i = 0; i < n; ++i) {
                    String path = originPaths.remove(originPaths.size() - 1);
                    threadPoolExecutor.execute(() -> {
                        taskDistribution("map", path);
                    });
                }
            } else {
                if (mapedPaths.size() > 0) {
                    log.info("启动线程分发reduce任务");
                    int n = mapedPaths.size();
                    for (int i = 0; i < n; ++i) {
                        String path = mapedPaths.remove(mapedPaths.size() - 1);
                        threadPoolExecutor.execute(() -> {
                            taskDistribution("reduce", path);
                        });
                    }
                }

            }
            while (threadPoolExecutor.getCompletedTaskCount() != threadPoolExecutor.getTaskCount()){
                Thread.sleep(1*1000);
            };
        }

        log.info("任务结束:最终完成统计"+reducedPaths.size()+"篇文章");
        System.in.read();
    }

    @Override
    public void setDir(String dir) {
        URL resource = Thread.currentThread().getContextClassLoader().getResource("");
        System.out.println("path:" + resource.getPath());
        File folder = new File(dir);
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    originPaths.add(dir +"/" + file.getName());
                }
            }
        }
    }

}
