package cc.lovezhy.raft.rpc;

import cc.lovezhy.raft.rpc.protocal.RpcRequest;
import cc.lovezhy.raft.rpc.protocal.RpcResponse;
import cc.lovezhy.raft.rpc.server.netty.NettyServer;
import cc.lovezhy.raft.rpc.server.netty.RpcService;
import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RpcServer {

    private static final Logger log = LoggerFactory.getLogger(RpcService.class);

    private  EndPoint DEFAULT_ENDPOINT = EndPoint.create("localhost", 5283);

    private  List<RpcProvider> providers;

    private  NettyServer nettyServer;

    private  Map<String, RpcProvider> serviceMap;

    private RpcService rpcService;

    public RpcServer() {
        this.serviceMap = Collections.synchronizedMap(Maps.newHashMap());
        this.providers = Collections.synchronizedList(Lists.newArrayList());
        this.rpcService = new RpcServerService();
    }

    public void start() {
        start(DEFAULT_ENDPOINT);
    }

    public void start(EndPoint endPoint) {
        nettyServer = new NettyServer(endPoint, rpcService);
        nettyServer.start();
        log.info("start rpc server endPoint={}", JSON.toJSONString(endPoint));
    }

    public void registerService(Class<?> serviceClass) {
        Preconditions.checkNotNull(serviceClass);
        Preconditions.checkState(serviceClass.getInterfaces().length == 1, "current rpcImpl class should have one interface and only one");
        RpcProvider provider = RpcProvider.create(serviceClass);
        serviceMap.put(serviceClass.getInterfaces()[0].getName(), provider);
        providers.add(provider);
        log.info("register service serviceClass={}", serviceClass);
    }

    public void close() {
        nettyServer.closeSync();
    }

    class RpcServerService implements RpcService {
        @Override
        public void onResponse(RpcResponse response) {
            throw new IllegalStateException();
        }

        @Override
        public void handleRequest(Channel channel, RpcRequest request) {
            RpcResponse rpcResponse = new RpcResponse();
            rpcResponse.setRequestId(request.getRequestId());
            RpcProvider provider = serviceMap.get(request.getClazz());
            Object responseObject = provider.invoke(request.getMethod(), request.getArgs());
            rpcResponse.setResponseBody(responseObject);
            channel.writeAndFlush(rpcResponse);
        }
    }

}