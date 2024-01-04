package com.huachuan.entity;

import lombok.Data;

@Data
public class HeartBeatResult {
    //follower返回的任期，对于leader会更新自己的任期
    private int term;

    //follower日志同步或心跳的结果
    private int success;

    //返回下次需要匹配的index
    private int nextMathIndex;
}
