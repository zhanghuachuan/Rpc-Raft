package com.huachuan.entity;

import lombok.Data;

import java.util.List;

/**
 * 心跳顺便同步日志
 */
@Data
public class HeartBeatParameter {
    //领导人的任期
    private int term;

    //leaderId
    private int leaderId;

    //检测日志同步的索引，目的是寻找与leader相同日志的索引
    private int prevLogIndex;

    private int prevLogTerm;

    //需要同步的多条日志，若没有日志需要同步，logs为空
    private List<Log> logs;

    //leader提交的最后一条日志的索引
    private int leaderCommitIndex;
}
