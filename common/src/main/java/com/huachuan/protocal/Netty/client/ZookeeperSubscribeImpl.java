package com.huachuan.protocal.Netty.client;


import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.RetryUntilElapsed;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class ZookeeperSubscribeImpl {
    //本地注册中心（可能会有线程安全问题，所以用ConcurrentHashMap）
    private static Map<String, List<String>> map = new ConcurrentHashMap<>();
    private static String nameSpace;
    private static String url;
    private static String port;
    private static int timeout;
    private static CuratorFramework client;

  public static void start() {
        Properties properties = new Properties();
      try {
          properties.load(new FileReader("src/main/resources/zookeeper.properties"));
      } catch (IOException e) {
          e.printStackTrace();
      }
      url = properties.getProperty("zk.server.url");
        port = properties.getProperty("zk.server.port");
        timeout = Integer.valueOf(properties.getProperty("zk.server.session.timeout"));
        nameSpace = properties.getProperty("zk.server.namespace");
        client = CuratorFrameworkFactory.builder()
                .connectString(url + ":" + port)
                .sessionTimeoutMs(timeout)
                .retryPolicy(new RetryUntilElapsed(2000,3000))
                .namespace(nameSpace)
                .build();
        client.start();

        //监听根节点，监听到有provider目录下有事件发生就做处理，更新到本地注册中心===异步
        final TreeCache treeCache = TreeCache.newBuilder(client, "/").setCacheData(true).build();
        treeCache.getListenable().addListener(new TreeCacheListener() {
            public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent treeCacheEvent) throws Exception {
                if(treeCacheEvent.getData() == null) return;
                String path = treeCacheEvent.getData().getPath();
                if(path == null || !path.contains("provider")) return;
                String[] nodes = path.split("/");
                if(nodes.length != 4) return;

                String key = nodes[1];
                String url = nodes[3];
               //provider下新增节点
                if(treeCacheEvent.getType() == TreeCacheEvent.Type.NODE_ADDED) {
                    if(map.containsKey(key)) {
                        map.get(key).add(url);
                    } else {
                        List<String> urls = new ArrayList<>();
                        urls.add(url);
                        map.put(key, urls);
                    }
                }
                //provider下移除节点
                if(treeCacheEvent.getType() == TreeCacheEvent.Type.NODE_REMOVED) {
                    if(map.containsKey(key)) {
                        map.get(key).remove(url);
                    }
                }
            }
  });

      try {
          treeCache.start();
      } catch (Exception e) {
          e.printStackTrace();
      }

  }





    public static List<String> getServerList(String serverName) {
        return map.get(serverName);
    }
}
