package com.huachuan.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class HeartBeatResult implements Serializable {
    @Serial
    private static final long serialVersionUID = -4789799144173087099L;
    //follower返回的任期，对于leader会更新自己的任期
    private int term;

    //follower日志同步或心跳的结果
    private int success;

    //返回下次需要匹配的index
    private int nextMatchIndex;

    //follower已经与leader同步完成的索引号
    private int ackIndex;
}
