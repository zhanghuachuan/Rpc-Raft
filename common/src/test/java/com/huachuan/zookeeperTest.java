package com.huachuan;

import com.huachuan.protocal.Netty.server.ZookeeperRegisterImpl;
import com.huachuan.protocal.Netty.client.ZookeeperSubscribeImpl;
import com.huachuan.entity.Url;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.RetryUntilElapsed;
import org.apache.zookeeper.data.Stat;
import org.junit.Test;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;

public class zookeeperTest {

    @Test
    public void test() throws Exception {
        CuratorFramework client = CuratorFrameworkFactory.builder().connectString("121.37.118.6:2181").sessionTimeoutMs(3000).retryPolicy(new RetryUntilElapsed(4000,3000)).namespace("zhanghuachuan").build();
        client.start();
        Stat stat = client.checkExists().forPath("/li");
        if(stat == null) System.out.println("??????????");
        else
            System.out.println("????????");

        System.in.read();

    }

    @Test
   public void getIp() {
        InetAddress localHost = getLocalHostExactAddress();
        System.out.println(localHost.getHostAddress());
    }

    public static InetAddress getLocalHostExactAddress() {
        try {
            InetAddress candidateAddress = null;

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface iface = networkInterfaces.nextElement();
                // ??????????????ip??????????????????????????????????????????????
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    // ????loopback????????????????????IPv4????IPv6 ??????????????????????true??
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            // ??????site-local?????????????? ??????????????
                            // ~~~~~~~~~~~~~????????????????????????????????ip??????~~~~~~~~~~~~~
                            return inetAddr;
                        }

                        // ??????site-local???? ????????????????????????
                        if (candidateAddress == null) {
                            candidateAddress = inetAddr;
                        }

                    }
                }
            }

            // ????????loopback????????????????????????????????????????????
            return candidateAddress == null ? InetAddress.getLocalHost() : candidateAddress;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    public void listenerTest() throws Exception {
        CuratorFramework client = CuratorFrameworkFactory.builder().connectString("121.37.118.6:2181").sessionTimeoutMs(3000).retryPolicy(new RetryUntilElapsed(4000,3000)).namespace("zhanghuachuan").build();
        client.start();
        final TreeCache watcher = TreeCache.newBuilder(client, "/").setCacheData(true).build();
        watcher.getListenable().addListener(new TreeCacheListener() {
            public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent treeCacheEvent) throws Exception {
                if(treeCacheEvent.getData() == null) return;
               String path = treeCacheEvent.getData().getPath();
                String[] nodes = path.split("/");
                System.out.println(nodes.length);
                for(String node : nodes) {
                    System.out.println(node);
                }
                }
            });
        watcher.start();
        System.in.read();
    }

    @Test
    public void getChildren() throws Exception {
        CuratorFramework client = CuratorFrameworkFactory.builder().connectString("121.37.118.6:2181").sessionTimeoutMs(3000).retryPolicy(new RetryUntilElapsed(4000,3000)).namespace("zhanghuachuan").build();
        client.start();
        List<String> strings = client.getChildren().forPath("/");
        for(String child : strings) {
            System.out.println(child);
        }

    }

    @Test
    public void registerTest() throws Exception {
        ZookeeperRegisterImpl register = new ZookeeperRegisterImpl();
        register.register("com.huachuan.add", new Url("127.0.0.1", 6668));
        System.in.read();
    }

    @Test
    public void subscribeTest() throws Exception {
        ZookeeperSubscribeImpl.start();
        while(true) {
            List<String> serverList = ZookeeperSubscribeImpl.getServerList("Echo.echo");
            if(serverList != null) {
                System.out.println("----------------");
                for(String server : serverList) {
                    System.out.println(server);
                }
                System.out.println("-------------------");
            }

            Thread.sleep(3000);
        }
    }
}
