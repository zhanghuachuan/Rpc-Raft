package com.huachuan.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class TransInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = -3792863627339496912L;
    public String className;
    public String methodName;
    public String version;
    public List<Class> parameterTypes;
    public List<Object> parameters;
    public Class returnType;
}
