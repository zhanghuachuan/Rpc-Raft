package com.huachuan;

import com.huachuan.consumer.BeanProxy;
import com.huachuan.messages.Message;
import com.huachuan.protocal.Netty.client.ZookeeperSubscribeImpl;
import org.junit.Test;

public class ClientTest {
    @Test
   public void client() throws Exception {
        //先订阅远端服务
        ZookeeperSubscribeImpl.start();
        while(true) {
            Message echo = (Message)BeanProxy.getBean(Message.class, "Echo");
            String res = echo.echo("huachuan");
            System.out.println(res);
            Thread.sleep(5*1000);
        }

    }
}
