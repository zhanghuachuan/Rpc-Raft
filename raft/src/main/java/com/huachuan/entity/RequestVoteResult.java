package com.huachuan.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class RequestVoteResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 33848517294976112L;
    //任期号，方便当前节点更新
    private int term;

    //请求投票结果 0,1
    private int voteGranted;
}
