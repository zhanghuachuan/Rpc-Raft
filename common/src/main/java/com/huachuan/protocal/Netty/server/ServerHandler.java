package com.huachuan.protocal.Netty.server;

import com.huachuan.entity.ReturnInfo;
import com.huachuan.entity.TransInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.lang.reflect.Method;
import java.util.List;

public class ServerHandler extends SimpleChannelInboundHandler<TransInfo> {
   private LocalRegister localRegister;

   ServerHandler(LocalRegister localRegister) {
       this.localRegister = localRegister;
   }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TransInfo transInfo) throws Exception {
        List<Object> list = localRegister.get(transInfo.className, transInfo.methodName);
        if(list == null) {
            System.out.println("调用失败：" + transInfo.className + ":" + transInfo.methodName);
            return;
        }
        Class cls = (Class)list.get(0);
       List<Class> types = transInfo.parameterTypes;
       Class[] typesArray = types.toArray(new Class[types.size()]);

       List<Object> parameters = transInfo.parameters;
        Object[] paras = parameters.toArray(new Object[parameters.size()]);
        Method method = cls.getMethod(transInfo.methodName, typesArray);
        Object invoke = method.invoke(list.get(1), paras);
        ReturnInfo info = new ReturnInfo();
        info.value = invoke;
        info.type = transInfo.returnType;
        channelHandlerContext.writeAndFlush(info);
    }
}
