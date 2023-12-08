package com.huachuan.provider;

import com.huachuan.protocal.Netty.server.LocalRegister;
import com.huachuan.protocal.Netty.server.NettyServerImpl;
import com.huachuan.protocal.Netty.server.ZookeeperRegisterImpl;

import java.util.List;

public class RpcServer {
   private int port;
   private String address;
   private NettyServerImpl nettyServer;
   LocalRegister localRegister = new LocalRegister();


   public RpcServer(String address) {
      String[] component = address.split(":");
      this.port = Integer.valueOf(component[1]);
      this.address = component[0];
      ZookeeperRegisterImpl.start();
   }

   public void start() {
      nettyServer = new NettyServerImpl();
      nettyServer.startServer(port, localRegister);
   }
   public void register(Class cls, Object single,  String serverName) throws Exception {
      localRegister.register(cls, single, serverName, address, port);
   }

   public void remove(String className, String serverName) {
      localRegister.remove(className, serverName);
   }

   public List<Object> get(String className, String serverName) {
      return localRegister.get(className, serverName);
   }

   public void closeServer() {
      nettyServer.closeServer();
   }
}
