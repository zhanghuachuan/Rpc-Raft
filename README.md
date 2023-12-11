## myrpc

### 项目简介

本项目基于netty实现通用的rpc框架，rpc分别实现了服务端与客户端，服务由接口+类的形式提供，服务端需要实现服务接口并将实现类注册到本地注册中心，此外，服务端还需要将提供的服务名称（类.方法）注册到远程注册中心zookeeper，客户端通过服务端的注册中心拿到提供服务的所有server信息，通过netty进行通信拿到服务端的执行结果，客户端提供代理模式供选择，代理接口直接提供了访问远程客户端的方法封装，直接调用就可获得服务对象，直接调用本地方法就可以实现rpc远程调用。在实现rpc的基础上，逐渐开启mit6.824的学习。该项目在实现上比较简化，只追求跑通理解概念，大体上比较容易理解，许多细节还有思考的空间。

### 使用方式

客户端和服务端应该知道他们的通信方式和内容，这里让客户端和服务端都拥有一个公共的接口方便客户端可以直接调用，本质上这里可以使用不同的数据交换协议，例如protobuf等

服务端

```
 //开启一个server，指定ip端口提供服务（这里使用ServerPool管理server，也可以直接创建）
 RpcServer rpcServer = ServerPool.get("127.0.0.1:8083");
 //注册服务，第一个参数是服务端提供的对应接口的具体实现类，第二个参数是提供服务的对象实例，这里new是让服务过程中不重复new对象，第三个参数代表提供服务的具体方法
 rpcServer.register(Echo.class, new Echo(), "echo");
 //启动服务
 rpcServer.start();
```

客户端

```
//使用BeanProxy直接代理实现对应接口，代理方法请求了远程server服务
Message echo = (Message)BeanProxy.getBean(Message.class, "Echo");
//直接调用就可获取服务端执行结果
String res = echo.echo("huachuan");
System.out.println(res);
```



### 项目架构

#### rpc架构

![rpc](https://github.com/996990870/myRpc/assets/54765732/5edd47a1-88a0-4548-ac7a-88a949bd9141)


上图可以描述为如下几个步骤

- server端向本地注册中心添加服务，添加服务后server会将该服务同步到zookeeper注册中心，zookeeper中的组织结构方式如上图所示。

- 客户端在初始化时，会启动一个本地缓存去获取zookeeper的注册信息，避免每次请求都会请求zookeeper，通过注册watcher，当zookeeper注册新的服务或者发生改变时客户端能够及时感知并更新本地缓存。

- 客户端请求服务时，首先去获取缓存中的服务注册信息，若获取失败，则说明没有提供该服务的server，获取成功后可以自定义负载均衡策略获取server，本实现只简单地随机获取server进行处理。

- 客户端可以使用实现的BeanProxy工具类获取服务的代理实现，代理实现封装了客户端调用服务的一系列过程。

  

#### mapreduce框架

完善中...

### 技术要点

- netty通信
- zookeeper注册中心
- 动态代理 
