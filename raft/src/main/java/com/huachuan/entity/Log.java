package com.huachuan.entity;

import lombok.Data;

@Data
public class Log {
    private int logTerm;
    private int logIndex;
    private String logContent;
}
