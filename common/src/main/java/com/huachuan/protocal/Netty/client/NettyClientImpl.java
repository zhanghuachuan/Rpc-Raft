package com.huachuan.protocal.Netty.client;

import com.huachuan.consumer.ClientPool;
import com.huachuan.entity.ReturnInfo;
import com.huachuan.entity.TransInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;


public class NettyClientImpl {

    private ClientHandler clientHandler = new ClientHandler();
    private EventLoopGroup group;
    private Bootstrap bootstrap;
    private String address;
    private int port;

   public NettyClientImpl(String address, int port){
       this.address = address;
       this.port = port;
        initClient(address, port);
    }

    public void initClient(String address, int port) {
        //客户端需要一个事件循环组
        group = new NioEventLoopGroup();
        try {

            //创建客户端启动对象
            //注意客户端使用的不是 ServerBootstrap 而是 Bootstrap
           bootstrap = new Bootstrap();
            //设置相关参数
            bootstrap.group(group) //设置线程组
                    .channel(NioSocketChannel.class) // 设置客户端通道的实现类(反射)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new ObjectDecoder(1024*1024, ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())))
                                    .addLast(new ObjectEncoder())
                                    .addLast(clientHandler); //加入自己的处理器
                        }
                    });

            System.out.println("客户端 ok..");
            //启动客户端去连接服务器端
            //关于 ChannelFuture 要分析，涉及到netty的异步模型
            ChannelFuture channelFuture = bootstrap.connect(address, port).sync();
            //给关闭通道进行监
            //channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
           // group.shutdownGracefully();
        }
    }

    public void closeClient() {
        group.shutdownGracefully();
    }

    public ReturnInfo send(TransInfo info) throws Exception {

        return clientHandler.sendRequest(info);
    }

    public int reConnect() {
       if (bootstrap == null) return 0;
       int result = 0;
       try {
            bootstrap.connect(address, port).sync();
            result = 1;
        } catch (InterruptedException e) {
           System.out.println("重连失败");
        }
       return result;
    }
}
