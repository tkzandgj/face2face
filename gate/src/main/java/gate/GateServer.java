package gate;
/**
 * Created by Qzy on 2016/1/28.
 * gate作为一个服务端，接收client的连接
 */
import gate.handler.GateServerHandler;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import protobuf.ParseRegistryMap;
import protobuf.code.PacketDecoder;
import protobuf.code.PacketEncoder;

import java.net.InetSocketAddress;


public class GateServer {
    private static final Logger logger = LoggerFactory.getLogger(GateServer.class);

    public static void startGateServer(int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel)
                            throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast("MessageDecoder", new PacketDecoder());
                        pipeline.addLast("MessageEncoder", new PacketEncoder());
                        pipeline.addLast("ClientMessageHandler", new GateServerHandler());
                    }
                });

        bindConnectionOptions(bootstrap);

        bootstrap.bind(new InetSocketAddress(port)).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future)
                    throws Exception {
                if (future.isSuccess()) {
                    //init Registry
                    ParseRegistryMap.initRegistry();
                    TransferHandlerMap.initRegistry();
                    logger.info("[GateServer] Started Successed, registry is complete, waiting for client connect...");
                } else {
                    logger.error("[GateServer] Started Failed, registry is incomplete");
                }
            }
        });
    }

    protected static void bindConnectionOptions(ServerBootstrap bootstrap) {

        bootstrap.option(ChannelOption.SO_BACKLOG, 1024);        // 初始化服务端的可连接队列
        bootstrap.childOption(ChannelOption.SO_LINGER, 0);       // 阻塞close()方法的调用时间，直到数据完全发送
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);  // 禁用TCP的NoDelay算法

        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true); // 调试用   多个服务复用同一个端口
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true); // 心跳机制暂时使用TCP选项，之后再自己实现

    }
}
