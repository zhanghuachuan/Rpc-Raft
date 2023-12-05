package com.huachuan.protocal.Netty.server;


import com.huachuan.entity.Url;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryUntilElapsed;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ZookeeperRegisterImpl {

    private static CuratorFramework client;
    private static String nameSpace;
    private static String url;
    private static String port;
    private static int timeout;


   public static void start() {
       if (client != null) return;
        Properties properties = new Properties();
       try {
           properties.load(new FileReader("src/main/resources/zookeeper.properties"));
       } catch (IOException e) {
           e.printStackTrace();
       }
       url = properties.getProperty("zk.server.url");
        port = properties.getProperty("zk.server.port");
        timeout = Integer.parseInt(properties.getProperty("zk.server.session.timeout"));
        nameSpace = properties.getProperty("zk.server.namespace");
        client = CuratorFrameworkFactory.builder()
                .connectString(url + ":" + port)
                .sessionTimeoutMs(timeout)
                .retryPolicy(new RetryUntilElapsed(2000,3000))
                .namespace(nameSpace)
                .build();
        client.start();
    }


    /**
     * 服务端需要注册到zookeeper，具体就是创建node节点并将信息存入节点中
     * 客户端需要维护本地的注册中心（map）与zookeeper注册中心信息的一致性
     * @param serverName
     * @param url
     */

    public static void register(String serverName, Url url) throws Exception {
        //查看当前服务是否已经被注册
        Stat stat = client.checkExists().forPath("/" + serverName);
        if(stat == null) {
            //服务还没有需要先创建
            client.create().creatingParentsIfNeeded().forPath("/" + serverName + "/provider");
            client.create().creatingParentsIfNeeded().forPath("/" + serverName + "/consumer");
        }

        //注册结点名字就是ip+端口
        String path = "/" + serverName + "/provider/" + url.getAddress() + ":" + url.getPort();
        Stat stat1 = client.checkExists().forPath(path);
        if(stat1 == null) client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);
    }

}
