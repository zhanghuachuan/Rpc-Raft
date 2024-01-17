package com.huahcuan;

import com.huachuan.RaftNode;
import com.huachuan.consumer.RpcClient;
import org.junit.Test;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class RaftTest {
    @Test
    public void node1() throws Exception {
        RaftNode node = new RaftNode("src/main/resources/raftConfig/raftConfig1.properties");
        node.run();
    }

    @Test
    public void node2() throws Exception {
        RaftNode node = new RaftNode("src/main/resources/raftConfig/raftConfig2.properties");
        node.run();
    }

    @Test
    public void node3() throws Exception {
        RaftNode node = new RaftNode("src/main/resources/raftConfig/raftConfig3.properties");
        node.run();
    }

    @Test
    public void node4() throws Exception {
        RaftNode node = new RaftNode("src/main/resources/raftConfig/raftConfig4.properties");
        node.run();
    }

    @Test
    public void node5() throws Exception {
        RaftNode node = new RaftNode("src/main/resources/raftConfig/raftConfig5.properties");
        node.run();
    }

    @Test
    public void appendLog() throws Exception {
        RpcClient client = new RpcClient("127.0.0.1:9002");
        for (int i = 0; i < 100; ++i) {
           int res = (int)client.request(RaftNode.class, "RaftNode", "appendLog", "hello");
           if (res == 1) {
               System.out.println("添加记录成功");
           } else {
               System.out.println("添加记录失败");
           }
           Thread.sleep(2000);
        }
        System.in.read();
    }
}
