package com.huachuan;

import lombok.Data;

import java.util.List;
@Data
public class RaftConfig {
    //集群机器地址
    private List<String> clusters;
    //集群一个周期等待时间
    private int seconds;
    //master向follower发送心跳周期
    private int heartBeatTime;
    //当前节点的地址
    private String address;
    //当前节点id
    private int id;
}
