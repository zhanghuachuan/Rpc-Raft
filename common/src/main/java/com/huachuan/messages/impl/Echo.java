package com.huachuan.messages.impl;

import com.huachuan.messages.Message;

public class Echo implements Message {
    @Override
    public String echo(String name) {
        System.out.println("hello:" + name);
        return "hello:" + name;
    }
}
