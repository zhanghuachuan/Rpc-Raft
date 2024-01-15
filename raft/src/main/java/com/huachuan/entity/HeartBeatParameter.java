package com.huachuan.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 心跳顺便同步日志
 */
@Data
public class HeartBeatParameter implements Serializable {
    @Serial
    private static final long serialVersionUID = -8450071281700730532L;
    //领导人的任期
    private int term;

    //leaderId
    private int leaderId;

    //检测日志同步的索引，目的是寻找与leader相同日志的索引
    private int prevLogIndex;

    private int prevLogTerm;

    //表明当前是否有需要进行同步的日志
    private int hasLog;

    //当前需要同步的日志
    private Log log;

    //leader提交的最后一条日志的索引
    private int leaderCommitIndex;
}
