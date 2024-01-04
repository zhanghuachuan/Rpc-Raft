package com.huachuan.entity;

import lombok.Data;

@Data
public class Log {
    private int logTerm;
    private int logIndex;
    private String logContent;

    public Log(int logTerm, int logIndex, String logContent) {
        this.logTerm = logTerm;
        this.logIndex = logIndex;
        this.logContent = logContent;
    }
}
