package com.huachuan.provider;

import com.huachuan.protocal.Netty.server.LocalRegister;
import com.huachuan.protocal.Netty.server.NettyServerImpl;
import com.huachuan.protocal.Netty.server.ZookeeperRegisterImpl;

public class RpcServer {
   private int port;
   private String address;
   private NettyServerImpl nettyServer;
   LocalRegister localRegister = new LocalRegister();


   public RpcServer(String address, int port) {
      this.port = port;
      this.address = address;
      ZookeeperRegisterImpl.start();
   }

   public void start() {
      nettyServer = new NettyServerImpl();
      nettyServer.startServer(port, localRegister);
   }
   public void register(Class cls, String serverName) throws Exception {
      localRegister.register(cls, serverName, address, port);
   }

   public void remove(String className, String serverName) {
      localRegister.remove(className, serverName);
   }

   public Class get(String className, String serverName) {
      return localRegister.get(className, serverName);
   }

   public void closeServer() {
      nettyServer.closeServer();
   }
}
