package cc.lovezhy.raft.server.web;

import cc.lovezhy.raft.server.log.DefaultCommand;
import cc.lovezhy.raft.server.node.RaftNode;
import com.google.common.base.Preconditions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHttpService extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(ClientHttpService.class);

    private HttpServer httpServer;
    private int port;

    private RaftNode.NodeMonitor nodeMonitor;
    private RaftNode.OuterService outerService;

    public ClientHttpService(RaftNode.NodeMonitor nodeMonitor, RaftNode.OuterService outerService, int port) {
        Preconditions.checkNotNull(nodeMonitor);
        Preconditions.checkNotNull(outerService);
        this.nodeMonitor = nodeMonitor;
        this.outerService = outerService;
        this.port = port;
        this.vertx = Vertx.vertx();
    }

    public void createHttpServer() {
        this.httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);
        /*
         * 查看节点状态
         */
        router.get("/status").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "application/json");
            response.end(nodeMonitor.getNodeStatus().toString());
        });

        /*
         * 向集群写value
         */
        router.post("/command").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            JsonObject bodyJson = routingContext.getBodyAsJson();
            DefaultCommand command = bodyJson.mapTo(DefaultCommand.class);
            boolean success = this.outerService.appendLog(command);
            JsonObject responseJson = new JsonObject();
            responseJson.put("success", success);
            response.putHeader("content-type", "application/json");
            response.end(responseJson.toString());
        });

        this.httpServer.requestHandler(router::accept).listen(this.port);
        log.info("start httpServer at port={}", this.port);
    }

    public void close() {
        try {
            this.httpServer.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
