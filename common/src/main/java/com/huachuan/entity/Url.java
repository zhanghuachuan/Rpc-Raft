package com.huachuan.entity;

public class Url {
    String address;
    int port;

    public Url(String address, int port){
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Url{" +
                "address='" + address + '\'' +
                ", port=" + port +
                '}';
    }

    public void setPort(int port) {
        this.port = port;
    }
}
