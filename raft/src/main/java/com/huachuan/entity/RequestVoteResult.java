package com.huachuan.entity;

import lombok.Data;

@Data
public class RequestVoteResult {
    //任期号，方便当前节点更新
    private int term;

    //请求投票结果 0,1
    private int voteGranted;
}
