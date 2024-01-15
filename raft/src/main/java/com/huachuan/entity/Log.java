package com.huachuan.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class Log implements Serializable {
    @Serial
    private static final long serialVersionUID = -2879565336173596749L;
    private int logTerm;
    private int logIndex;
    private String logContent;

    public Log(int logTerm, int logIndex, String logContent) {
        this.logTerm = logTerm;
        this.logIndex = logIndex;
        this.logContent = logContent;
    }
}
