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
    private int status;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        context = ctx; //因为我们在其它方法会使用到 ctx
        status = 1;
    }

    @Override
    protected synchronized void channelRead0(ChannelHandlerContext channelHandlerContext, ReturnInfo returnInfo) throws Exception {
        this.result = returnInfo;
        notify();
    }


    @Override
    public synchronized void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(cause);
        result = null;
        ctx.close();
        status = 0;
        notify();
    }

    @Override
    public synchronized void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
        InetSocketAddress address = (InetSocketAddress)ctx.channel().remoteAddress();
        ClientPool.remove(address.getAddress().getHostAddress() +":" + address.getPort());
        status = 0;
        notify();
    }


    public synchronized ReturnInfo sendRequest(TransInfo info) throws Exception {
        while (context == null){

        }
        context.writeAndFlush(info);
        if (status == 0) return null;
        wait();
        return result;
    }

    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
      this.status = status;
    }
}
