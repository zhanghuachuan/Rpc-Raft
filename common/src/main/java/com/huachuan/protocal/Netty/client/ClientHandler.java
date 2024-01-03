package com.huachuan.protocal.Netty.client;

import com.huachuan.consumer.ClientPool;
import com.huachuan.entity.ReturnInfo;
import com.huachuan.entity.TransInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetSocketAddress;

public class ClientHandler extends SimpleChannelInboundHandler<ReturnInfo>{

    private volatile ChannelHandlerContext context;//上下文
    private ReturnInfo result; //返回的结果

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        context = ctx; //因为我们在其它方法会使用到 ctx
    }

    @Override
    protected synchronized void channelRead0(ChannelHandlerContext channelHandlerContext, ReturnInfo returnInfo) throws Exception {
        this.result = returnInfo;
        notify();
    }


    @Override
    public synchronized void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
        InetSocketAddress address = (InetSocketAddress)ctx.channel().remoteAddress();
        System.out.println(address.getPort()); //得到的是netty程序运行在电脑上的port
        System.out.println(address.getAddress());
        ClientPool.remove(address.getAddress().getHostAddress() +":" + address.getPort());
    }


    public synchronized ReturnInfo sendRequest(TransInfo info) throws Exception {
        while (context == null){

        }
        context.writeAndFlush(info);
        System.out.println("等待结果中。。。");
        wait();
        System.out.println("返回结果：" + result);
        return result;
    }
}
