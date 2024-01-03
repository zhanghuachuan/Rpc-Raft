package com.huachuan;
import com.huachuan.consumer.RpcClient;
import com.huachuan.entity.*;
import com.huachuan.protocal.Netty.server.ZookeeperRegisterImpl;
import com.huachuan.provider.RpcServer;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class RaftNode {
    private static Logger log = Logger.getLogger(RaftNode.class);

    private RaftConfig raftConfig;

    private RpcServer rpcServer;

    private List<String> clients;

    private Map<String, RpcClient> clientMap;
    //当前节点任期
    private int currentTerm;

    //当前节点角色 1:leader 2:follower 3:candidate
    private int role;

    //在一个新任期内，节点默认有一票，如果当前节点是candidate 该字段作为统计的的票数
    private int tickets;

    //当前集群的leaderId
    private int leaderId;

    //当前节点id
    private int id;

    //当前节点最后一条日志index
    private int lastLogIndex;

    //当前节点最后一条日志任期号
    private int lastLogTerm;

    //当前节点已经提交的日志index
    private int commitedIndex;

    //当前日志
    private List<Log> logs;


    public RaftNode(String configPath) throws IOException {
        this.id = id;
        currentTerm = 0;
        role = 3;
        leaderId = -1;
        logs = new ArrayList<>();
        lastLogIndex = -1;
        lastLogTerm = -1;
        commitedIndex = -1;
        Properties properties = new Properties();
        properties.load(new FileReader(RaftNode.class.getResource(configPath).getPath()));
        raftConfig = new RaftConfig();
        raftConfig.setId(Integer.parseInt(properties.getProperty("id")));
        raftConfig.setClusters(Arrays.asList(properties.getProperty("cluster").split(",")));
        raftConfig.setAddress(properties.getProperty("address"));
        raftConfig.setSeconds(Integer.parseInt(properties.getProperty("seconds")));
        raftConfig.setHeartBeatTime(Integer.parseInt(properties.getProperty("heartBeatTime")));
    }

    void run() throws Exception {
        rpcServer = new RpcServer(raftConfig.getAddress());
        rpcServer.register(RaftNode.class, this, "voteFor");
        rpcServer.register(RaftNode.class, this, "heartBeatRecive");

        //创建连接其他节点的客户端
        clients = raftConfig.getClusters();
        clientMap = new HashMap<>();
        for (int i = 0; i < clients.size(); i++) {
            String url = clients.get(i);
            if (raftConfig.getAddress().equals(url)) continue;
            clientMap.put(url, new RpcClient(url));
        }


    }

    //为其他candidate投票，投票成功返回1，失败返回0
    public synchronized RequestVoteResult voteFor(RequestVoteParameter parameter) {
        RequestVoteResult result = new RequestVoteResult();
        result.setTerm(currentTerm);
        result.setVoteGranted(0);
        if (role == 3 || tickets == 0 || currentTerm > parameter.getTerm()) return result;
        if (lastLogTerm > parameter.getLastLogTerm()) return result;
        if (lastLogTerm == parameter.getLastLogTerm() && lastLogIndex > parameter.getLastLogIndex()) return result;
        --tickets;
        result.setVoteGranted(1);
        return result;
    }

    //请求其他节点投票
    public int requestVote() {
       return 1;
    }

    //如果当前节点是leader时需要对其他节点发送心跳
    public int heartBeat() {

        return 1;
    }

    public HeartBeatResult heartBeatRecive(HeartBeatParameter parameter) {
        HeartBeatResult result = new HeartBeatResult();
        if (parameter.getTerm() > currentTerm) currentTerm = parameter.getTerm();
        result.setTerm(currentTerm);
        result.setSuccess(0);
        if (leaderId != parameter.getLeaderId()) leaderId = parameter.getLeaderId();
        if (parameter.getPrevLogIndex() >= logs.size()) return result;
        if (logs.get(parameter.getPrevLogIndex()).getLogTerm() == parameter.getPrevLogTerm()) result.setSuccess(1);
        List<Log> tempLogs = parameter.getLogs();
        //更新日志
        if (tempLogs.size() > 0) {
            for (Log log : tempLogs) {
                if (log.getLogIndex() >= logs.size()) logs.add(log);
                else logs.set(log.getLogIndex(), log);
            }
        }
        return result;
    }



}
