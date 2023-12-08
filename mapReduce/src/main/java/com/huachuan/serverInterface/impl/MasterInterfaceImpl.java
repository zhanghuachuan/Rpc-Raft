package com.huachuan.serverInterface.impl;

import com.huachuan.consumer.RpcClient;
import com.huachuan.serverInterface.MasterInterface;
import com.huachuan.serverInterface.WorkerInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MasterInterfaceImpl implements MasterInterface {
    private Object mapTaskCountLock = new Object();
    private int mapTaskCount;
    private Object reduceTaskCountLock = new Object();
    private int reduceTaskCount;

    final Lock lock = new ReentrantLock();//锁对象
    final Condition empty  = lock.newCondition();//写线程条件变量

    //存储所有的worker
    Map<String, RpcClient> workerMap = new ConcurrentHashMap<>();
    //空闲worker的ip地址
    List<String> avaliableWorkers = Collections.synchronizedList(new ArrayList<>());
    //所有worker的ip地址
    List<String> workers = Collections.synchronizedList(new ArrayList<>());

    public MasterInterfaceImpl(int mapTaskCount, int reduceTaskCount) {
        this.mapTaskCount = mapTaskCount;
        this.reduceTaskCount = reduceTaskCount;
    }

    public void decrementMapTaskCount() {
        synchronized (mapTaskCountLock) {
            --mapTaskCount;
        }
    }
    public void incrementMapTaskCount() {
        synchronized (mapTaskCountLock) {
            ++mapTaskCount;
        }
    }

    public void decrementReduceTaskCount() {
        synchronized (reduceTaskCountLock) {
            --reduceTaskCount;
        }
    }
    public void incrementReduceTaskCount() {
        synchronized (reduceTaskCountLock) {
            ++reduceTaskCount;
        }
    }

    @Override
    public int getMapTask() {
        return mapTaskCount;
    }

    @Override
    public int getReduceTask() {
        return reduceTaskCount;
    }

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

    public  void taskDistribution(String type){
        String url = "";
        lock.lock();
      try {
        while (avaliableWorkers.size() <= 0) {
          empty.await();
        }

        if (type.equals("map")) decrementMapTaskCount();
        else decrementReduceTaskCount();
        url = avaliableWorkers.get(avaliableWorkers.size() - 1);
        avaliableWorkers.remove(avaliableWorkers.size() - 1);
        lock.unlock();
        workerMap.get(url).request(WorkerInterface.class, "WorkerInterfaceImpl", "handleTask", type);
        } catch (Exception e) { //远程worker处理失败， 任务需要重新被执行
            System.out.println("worker任务处理失败");
            if (type.equals("map")) incrementMapTaskCount();
            else incrementReduceTaskCount();
        } finally { //无论处理成功还是失败，都需要将其加入空闲列表，去除无效连接让heartBeat做
            avaliableWorkers.add(url);
        }
        System.out.println("worker任务处理成功");
    }

    //每5s对所有的服务提供者检查一次
    public void heartBeat() throws InterruptedException {
        while (true) {
            for(String url : workers) {
                try {
                    String res = (String) workerMap.get(url).request(WorkerInterface.class, "WorkerInterfaceImpl", "heartBeat");
                    System.out.println("收到来自" + url + "的心跳信息：" + res);
                } catch (Exception e) {
                    workers.remove(url);
                    avaliableWorkers.remove(url);
                    workerMap.get(url).closeClient();
                    workerMap.remove(url);
                    System.out.println(url + "断开连接");
                }
            }
            Thread.sleep(5*1000);
        }
    }

}
