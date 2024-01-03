package com.huachuan.entity;

import lombok.Data;

@Data
public class RequestVoteParameter {
    //候选人的任期号
    private int term;

    //候选人id
    private int candidateId;

    //最后一条日志的index
    private int lastLogIndex;
    
    //最后一条日志的任期号
    private int lastLogTerm;
}
