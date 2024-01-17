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
    private volatile int currentTerm;

    //当前节点自旋剩余时间
    private volatile int restTime;

    //当前节点角色 1:leader 2:follower 3:candidate
    private volatile int role;

    //在一个新任期内，节点默认有一票，如果当前节点是candidate 该字段作为统计的的票数
    private volatile int tickets;

    //当前集群的leaderId
    private volatile int leaderId;

    //当前节点id
    private volatile int id;

    //当前节点最后一条日志index
    private volatile int lastLogIndex;

    //当前节点最后一条日志任期号
    private volatile int lastLogTerm;

    //当前节点已经提交的日志index
    private volatile int commitedIndex;

    //当前日志
    private volatile List<Log> logs;

    //如果当前节点是leader，保存对其他节点的heartBeatParameter
    Map<String, HeartBeatParameter> heartBeatParameterMap;

    public RaftNode(String configPath) throws IOException {
        currentTerm = 0;
        role = 2;
        leaderId = -1;
        logs = new ArrayList<>();
        logs.add(new Log(0,0,"start"));
        lastLogIndex = 0;
        lastLogTerm = 0;
        commitedIndex = 0;
        tickets = 1;
        Properties properties = new Properties();
        //properties.load(new FileReader(RaftNode.class.getResource(configPath).getPath()));
        properties.load(new FileReader(configPath));
        raftConfig = new RaftConfig();
        raftConfig.setId(Integer.parseInt(properties.getProperty("id")));
        raftConfig.setClusters(Arrays.asList(properties.getProperty("cluster").split(",")));
        raftConfig.setAddress(properties.getProperty("address"));
        raftConfig.setSeconds(Integer.parseInt(properties.getProperty("seconds")));
        raftConfig.setHeartBeatTime(Integer.parseInt(properties.getProperty("heartBeatTime")));
        restTime = raftConfig.getSeconds();
        id = raftConfig.getId();
    }

    private RpcClient getClient(String url) {
        RpcClient client = clientMap.get(url);
        if (client == null || client.getStatus() == 0) {
            try {
                client = new RpcClient(url);
                clientMap.put(url, client);
            } catch (Exception e) {
                System.out.println(url + "还未就绪");
                client = null;
            }
        }
        return client;
    }

    private synchronized void resetRestTime() {
        restTime = raftConfig.getSeconds();
    }

   public void run() throws Exception {
        rpcServer = new RpcServer(raftConfig.getAddress());
        rpcServer.register(RaftNode.class, this, "voteFor");
        rpcServer.register(RaftNode.class, this, "heartBeatRecive");
       rpcServer.register(RaftNode.class, this, "appendLog");
        rpcServer.start();
        //创建连接其他节点的客户端
        clients = raftConfig.getClusters();
        clientMap = new HashMap<>();
        heartBeatParameterMap = new HashMap<>();
        for (int i = 0; i < clients.size(); i++) {
            String url = clients.get(i);
            if (raftConfig.getAddress().equals(url)) continue;
            RpcClient client = null;
            try {
                client = new RpcClient(url);
            } catch (Exception e) {
                System.out.println(url + "还未就绪");
            }
            if (client != null) clientMap.put(url, client);
            HeartBeatParameter heartBeatParameter = new HeartBeatParameter();
            heartBeatParameter.setTerm(-1);
            heartBeatParameterMap.put(url, heartBeatParameter);
        }
        while (true) {
            resetRestTime();
            while (restTime >= 0) {
                 Thread.sleep(1000);
                 --restTime;
                System.out.println("剩余restTime：" + restTime);
            }
            leaderId = -1;
            role = 3;
            ++currentTerm;
            tickets = 1;
            int res = requestVote();
            if (res == 1) {
                System.out.println(id + "成为leader");
                leaderId = id;
                role = 1;
                heartBeat();
            }
            Thread.sleep(5000);
        }
    }

    //为其他candidate投票，投票成功返回1，失败返回0
    public synchronized RequestVoteResult voteFor(RequestVoteParameter parameter) {
        System.out.println("处理来自" +parameter.getCandidateId() + "的投票请求");
        RequestVoteResult result = new RequestVoteResult();
        result.setTerm(currentTerm);
        result.setVoteGranted(0);
        if (currentTerm < parameter.getTerm()) {
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
        System.out.println(id + "发起投票");
        for(String url : clients) {
            if (role != 3) return 0;
            if(url.equals(raftConfig.getAddress())) continue;
            RpcClient client = getClient(url);
            if (client == null || client.getStatus() == 0) continue;
            RequestVoteParameter parameter = new RequestVoteParameter();
            parameter.setTerm(currentTerm);
            parameter.setCandidateId(id);
            parameter.setLastLogIndex(lastLogIndex);
            parameter.setLastLogTerm(lastLogTerm);
            RequestVoteResult result = null;
            try {
                result = (RequestVoteResult)client.request(RaftNode.class, "RaftNode", "voteFor", parameter);
            } catch (Exception e) {
                System.out.println(url + "断开连接");
            }
           if (result == null) {
               System.out.println("请求投票结果为空");
               continue;
           }
           //如果已经有leader选举成功
           if (result.getTerm() > currentTerm) {
               role = 2;
               currentTerm = result.getTerm();
               tickets = 1;
               return 0;
           }
           else {
               if (result.getVoteGranted() == 1) {
                   ++tickets;
                   System.out.println(id + "获得了来自" + url + "的投票");
               }
           }
        }
        //选举成功
        if (tickets > raftConfig.getClusters().size() / 2) return 1;
        System.out.println(id + "未能选举成功");
        return 0;
    }

    //如果当前节点是leader时需要对其他节点发送心跳
    public void heartBeat() throws Exception {
        for (String url : clients) {
            if(url.equals(raftConfig.getAddress())) continue;
            HeartBeatParameter heartBeatParameter = heartBeatParameterMap.get(url);
            heartBeatParameter.setPrevLogIndex(lastLogIndex);
            heartBeatParameter.setPrevLogTerm(lastLogTerm);
            heartBeatParameter.setAckIndex(0);
            heartBeatParameter.setHasLog(0);
        }
        while(leaderId == id) {
            int alive = raftConfig.getClusters().size();
            List<Integer> commitedIndexList = new ArrayList<>();
            for (String url : clients) {
                if(url.equals(raftConfig.getAddress())) continue;
                RpcClient client = getClient(url);
                if (client == null || client.getStatus() == 0) {
                    --alive;
                    continue;
                }
                HeartBeatParameter heartBeatParameter = heartBeatParameterMap.get(url);
                heartBeatParameter.setLeaderId(leaderId);
                heartBeatParameter.setTerm(currentTerm);
                heartBeatParameter.setLeaderCommitIndex(commitedIndex);
                HeartBeatResult heartBeatResult = null;
                try {
                    heartBeatResult = (HeartBeatResult) client.request(RaftNode.class, "RaftNode", "heartBeatRecive", heartBeatParameter);
                }catch (Exception e) {
                    System.out.println(url + "断开连接");
                }
                if (heartBeatResult == null) continue;

                if (heartBeatResult.getTerm() > currentTerm) {
                    role = 2;
                    currentTerm = heartBeatResult.getTerm();
                    tickets = 1;
                    leaderId = -1;
                    return;
                }
                heartBeatParameter.setAckIndex(heartBeatResult.getAckIndex());
                if (heartBeatParameter.getAckIndex() >= commitedIndex) {
                    commitedIndexList.add(heartBeatParameter.getAckIndex());
                }
                int index = heartBeatResult.getNextMatchIndex();
                if (index > lastLogIndex) {
                    heartBeatParameter.setHasLog(0);
                    System.out.println("index > lastLogIndex");
                    continue;
                }
                System.out.println("index = " + index);
                heartBeatParameter.setPrevLogIndex(index);
                heartBeatParameter.setPrevLogTerm(logs.get(index).getLogTerm());
                if (heartBeatResult.getSuccess() == 0) {
                    heartBeatParameter.setHasLog(0);
                } else {
                    heartBeatParameter.setLog(logs.get(index));
                    heartBeatParameter.setHasLog(1);
                }
            }
            //检查是否有可以提交的内容
            if (commitedIndexList.size() >= (raftConfig.getClusters().size() / 2)) {
                Collections.sort(commitedIndexList, Collections.reverseOrder());
                commitedIndex = commitedIndexList.get(raftConfig.getClusters().size() / 2 - 1);
                System.out.println("最新提交索引：" + commitedIndex);
            }
            commitedIndexList.clear();
            if (alive < (raftConfig.getClusters().size() / 2 + 1)) return;
            Thread.sleep(raftConfig.getHeartBeatTime() * 1000);
        }
    }

    public HeartBeatResult heartBeatRecive(HeartBeatParameter parameter) {
        System.out.println(id + "接收到心跳请求，开始重新计时");
        resetRestTime();
        HeartBeatResult result = new HeartBeatResult();
        if (parameter.getTerm() > currentTerm) {
            currentTerm = parameter.getTerm();
            role = 2;
            leaderId = parameter.getLeaderId();
        }
        result.setTerm(currentTerm);
        result.setAckIndex(Math.max(parameter.getAckIndex(), commitedIndex));
        result.setSuccess(0);
        result.setNextMatchIndex(Math.min(parameter.getPrevLogIndex() - 1, lastLogIndex));
        if (leaderId != parameter.getLeaderId()) leaderId = parameter.getLeaderId();
        if (parameter.getHasLog() == 0) {
            if (parameter.getPrevLogIndex() >= logs.size()) return result;
            if (logs.get(parameter.getPrevLogIndex()).getLogTerm() == parameter.getPrevLogTerm()) {
                result.setSuccess(1);
                result.setNextMatchIndex(parameter.getPrevLogIndex()+ 1);
                return result;
            }
            return result;
        }

        Log tempLog = parameter.getLog();
        //判断当前日志是否是遗留的日志，比如节点断开后消除了日志，但在leader端保存有将要写入的日志
        if (tempLog.getLogIndex() > lastLogIndex + 1) return result;
        //更新日志
        System.out.println("有日志需要同步：" + tempLog.getLogIndex() + "-" + tempLog.getLogTerm() + "-" + tempLog.getLogContent());
        if (tempLog.getLogIndex() >= logs.size()) {
            logs.add(tempLog);
            lastLogIndex = tempLog.getLogIndex();
            lastLogTerm = tempLog.getLogTerm();
            System.out.println(id + "添加日志：" + tempLog.getLogContent());
        }
        else {
            logs.set(tempLog.getLogIndex(), tempLog);
            if (tempLog.getLogIndex() == logs.size() - 1) {
                lastLogIndex = tempLog.getLogIndex();
                lastLogTerm = tempLog.getLogTerm();
            }
        }

        result.setAckIndex(tempLog.getLogIndex());
        commitedIndex = Math.min(parameter.getLeaderCommitIndex(), tempLog.getLogIndex());
        System.out.println(id + "提交确认:" + commitedIndex);

        result.setSuccess(1);
        result.setNextMatchIndex(tempLog.getLogIndex() + 1);
        return result;
    }

    public synchronized int appendLog(String content) throws Exception {
        //leader还未选举成功，拒绝日志写入请求
        if (leaderId == -1) {
            System.out.println("leader未选举成功，拒绝日志请求");
            return 0;
        }
        //如果当前节点不是leader，转发到leader节点进行日志写入
        if (leaderId != id) {
            System.out.println("转发日志append请求");
            //获取leader的url
            String url = raftConfig.getClusters().get(leaderId - 1);
            RpcClient client = getClient(url);
            if (client == null || client.getStatus() == 0) return 0;
            return (int) client.request(RaftNode.class, "RaftNode", "appendLog", content);
        } else {
            //如果当前节点是leader，需要写入到当前节点的日志中，并更新当前最新日志的索引号和任期号
           Log tempLog = new Log(currentTerm, lastLogIndex + 1, content);
           logs.add(tempLog);
           ++lastLogIndex;
           lastLogTerm = currentTerm;
            System.out.println(id + "写入日志：" + tempLog.getLogIndex() + "-" + tempLog.getLogTerm() + "-" + tempLog.getLogContent());
        }
        return 1;
    }



}
