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

    //当前节点自旋剩余时间
    private int restTime;

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

    //如果当前节点是leader，保存对其他节点的heartBeatParameter
    Map<String, HeartBeatParameter> heartBeatParameterMap;


    public RaftNode(String configPath) throws IOException {
        this.id = id;
        currentTerm = 0;
        role = 2;
        leaderId = -1;
        logs = new ArrayList<>();
        logs.add(new Log(0,0,"start"));
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
        restTime = raftConfig.getSeconds();
    }

    void run() throws Exception {
        rpcServer = new RpcServer(raftConfig.getAddress());
        rpcServer.register(RaftNode.class, this, "voteFor");
        rpcServer.register(RaftNode.class, this, "heartBeatRecive");

        //创建连接其他节点的客户端
        clients = raftConfig.getClusters();
        clientMap = new HashMap<>();
        heartBeatParameterMap = new HashMap<>();
        for (int i = 0; i < clients.size(); i++) {
            String url = clients.get(i);
            if (raftConfig.getAddress().equals(url)) continue;
            clientMap.put(url, new RpcClient(url));
            HeartBeatParameter heartBeatParameter = new HeartBeatParameter();
            heartBeatParameter.setTerm(-1);
            heartBeatParameterMap.put(url, heartBeatParameter);
        }




    }

    //为其他candidate投票，投票成功返回1，失败返回0
    public synchronized RequestVoteResult voteFor(RequestVoteParameter parameter) {
        RequestVoteResult result = new RequestVoteResult();
        result.setTerm(currentTerm);
        result.setVoteGranted(0);
        if (currentTerm <= parameter.getTerm()) {
            role = 2;
            //任期变了，在新任期的票数重新刷新
            if (currentTerm < parameter.getTerm()) tickets = 1;
            currentTerm = parameter.getTerm();
        }

        if (role == 3 || tickets == 0 || currentTerm > parameter.getTerm()) return result;
        if (lastLogTerm > parameter.getLastLogTerm()) return result;
        if (lastLogTerm == parameter.getLastLogTerm() && lastLogIndex > parameter.getLastLogIndex()) return result;
        --tickets;
        result.setVoteGranted(1);
        return result;
    }

    //请求其他节点投票
    public int requestVote() throws Exception {
        for(String url : clients) {
            if(url.equals(raftConfig.getAddress())) continue;
            RpcClient client = clientMap.get(url);
            RequestVoteParameter parameter = new RequestVoteParameter();
            parameter.setTerm(currentTerm);
            parameter.setCandidateId(id);
            parameter.setLastLogIndex(lastLogIndex);
            parameter.setLastLogTerm(lastLogTerm);
            RequestVoteResult result = (RequestVoteResult)client.request(RaftNode.class, "RaftNode", "voteFor", parameter);
           //如果已经有leader选举成功
           if (result.getTerm() > currentTerm) {
               role = 2;
               currentTerm = result.getTerm();
               tickets = 1;
               return 0;
           }
           else {
               if (result.getVoteGranted() == 1) ++tickets;
           }
        }
        //选举成功
        if (tickets > raftConfig.getClusters().size() / 2) return 1;
        return 0;
    }

    //如果当前节点是leader时需要对其他节点发送心跳
    public void heartBeat() throws Exception {
        for (String url : clients) {
            if(url.equals(raftConfig.getAddress())) continue;
            HeartBeatParameter heartBeatParameter = heartBeatParameterMap.get(url);
            heartBeatParameter.setPrevLogIndex(lastLogIndex);
            heartBeatParameter.setPrevLogTerm(lastLogTerm);
            heartBeatParameter.setHasLog(0);
        }
        while(leaderId == id) {
            for (String url : clients) {
                if(url.equals(raftConfig.getAddress())) continue;
                RpcClient client = clientMap.get(url);
                HeartBeatParameter heartBeatParameter = heartBeatParameterMap.get(url);
                heartBeatParameter.setLeaderId(leaderId);
                heartBeatParameter.setTerm(currentTerm);
                heartBeatParameter.setLeaderCommitIndex(commitedIndex);
               HeartBeatResult heartBeatResult = (HeartBeatResult) client.request(RaftNode.class, "RaftNode", "heartBeatRecive", heartBeatParameter);
                if (heartBeatResult.getTerm() > currentTerm) {
                    role = 2;
                    currentTerm = heartBeatResult.getTerm();
                    tickets = 1;
                    return;
                }
                int index = heartBeatResult.getNextMathIndex();
                if (index >= logs.size()) {
                    heartBeatParameter.setHasLog(0);
                    continue;
                }
                heartBeatParameter.setPrevLogIndex(index);
                heartBeatParameter.setPrevLogTerm(logs.get(index).getLogTerm());
                if (heartBeatResult.getSuccess() == 0) {
                    heartBeatParameter.setHasLog(0);
                } else {
                    heartBeatParameter.setLog(logs.get(index));
                    heartBeatParameter.setHasLog(1);
                }
            }
        }
        Thread.sleep(raftConfig.getHeartBeatTime() * 1000);
    }

    public HeartBeatResult heartBeatRecive(HeartBeatParameter parameter) {
        HeartBeatResult result = new HeartBeatResult();
        if (parameter.getTerm() > currentTerm) {
            currentTerm = parameter.getTerm();
            role = 2;
            leaderId = parameter.getLeaderId();
        }
        result.setTerm(currentTerm);
        result.setSuccess(0);
        result.setNextMathIndex(parameter.getPrevLogIndex() - 1);
        if (leaderId != parameter.getLeaderId()) leaderId = parameter.getLeaderId();
        if (parameter.getPrevLogIndex() >= logs.size()) return result;
        if (logs.get(parameter.getPrevLogIndex()).getLogTerm() == parameter.getPrevLogTerm()) result.setSuccess(1);
        else return result;
        Log tempLog = parameter.getLog();
        //更新日志
        if (parameter.getHasLog() == 1) {
            if (tempLog.getLogIndex() >= logs.size()) logs.add(tempLog);
            else logs.set(tempLog.getLogIndex(), tempLog);
            result.setNextMathIndex(tempLog.getLogIndex() + 1);
        } else {
            result.setNextMathIndex(tempLog.getLogIndex() + 1);
        }
        return result;
    }



}
