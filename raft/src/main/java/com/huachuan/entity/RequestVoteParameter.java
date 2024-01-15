package com.huachuan.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class RequestVoteParameter implements Serializable {
    @Serial
    private static final long serialVersionUID = 7392878821362737391L;
    //候选人的任期号
    private int term;

    //候选人id
    private int candidateId;

    //最后一条日志的index
    private int lastLogIndex;
    
    //最后一条日志的任期号
    private int lastLogTerm;
}
