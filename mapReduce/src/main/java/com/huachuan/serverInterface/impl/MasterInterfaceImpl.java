package com.huachuan.serverInterface.impl;

import com.huachuan.consumer.RpcClient;
import com.huachuan.serverInterface.MasterInterface;
import com.huachuan.serverInterface.WorkerInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MasterInterfaceImpl implements MasterInterface {
    private AtomicInteger mapTaskCount;
    private AtomicInteger reduceTaskCount;


    //存储所有的worker
    Map<String, RpcClient> workerMap = new ConcurrentHashMap<>();
    //空闲worker的ip地址
    List<String> avaliableWorkers = Collections.synchronizedList(new ArrayList<>());
    //所有worker的ip地址
    List<String> workers = Collections.synchronizedList(new ArrayList<>());

    public MasterInterfaceImpl(int mapTaskCount, int reduceTaskCount) {
        this.mapTaskCount = new AtomicInteger(mapTaskCount);
        this.reduceTaskCount = new AtomicInteger(reduceTaskCount);
    }

    @Override
    public AtomicInteger getMapTask() {
        return mapTaskCount;
    }

    @Override
    public AtomicInteger getReduceTask() {
        return reduceTaskCount;
    }

    @Override
    public int getOnBusy() {
        return workers.size() - avaliableWorkers.size();
    }

    public int register(String address) {
        if (workerMap.containsKey(address)) return 0;
        workerMap.put(address, new RpcClient(address));
        avaliableWorkers.add(address);
        workers.add(address);
        return 1;
    }

    @Override
    public int getWorkerServerCount() {
        return avaliableWorkers.size();
    }

    public void taskDistribution(String type) {
        if (avaliableWorkers.size() != 0) {
            if (type.equals("map")) mapTaskCount.decrementAndGet();
            else reduceTaskCount.decrementAndGet();
           String url = avaliableWorkers.get(avaliableWorkers.size() - 1);
           avaliableWorkers.remove(avaliableWorkers.size() - 1);
           try {
               workerMap.get(url).request(WorkerInterface.class, "WorkerInterfaceImpl", "handleTask", type);
           } catch (Exception e) { //远程worker处理失败， 任务需要重新被执行
               System.out.println("worker任务处理失败");
               if (type.equals("map")) mapTaskCount.getAndIncrement();
               else reduceTaskCount.getAndIncrement();
            } finally { //无论处理成功还是失败，都需要将其加入空闲列表，去除无效连接让heartBeat做
               avaliableWorkers.add(url);
           }
            System.out.println("worker任务处理成功");
        }
    }

    //每5s对所有的服务提供者检查一次
    public void heartBeat() throws InterruptedException {
        while (true) {
            for(String url : workers) {
                try {
                    String res = (String) workerMap.get(url).request(WorkerInterface.class, "WorkerInterfaceImpl", "heartBeat");
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
