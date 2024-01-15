package com.huahcuan;

import com.huachuan.RaftNode;
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
}
