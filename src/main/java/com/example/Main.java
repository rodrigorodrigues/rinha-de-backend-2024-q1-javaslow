package com.example;

import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.NodeState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executors;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static void main(String[] args) throws IOException {
        HttpServer httpServer = HttpServer
                .create(new InetSocketAddress(Integer.parseInt(System.getenv("SERVER_PORT"))), 0);

        httpServer.createContext("/clientes", new CreateTransactionHandler(new AccountRepository(), new TransactionRepository()));
        httpServer.createContext("/healthcheck", exchange -> {
            Collection<Node> nodes = CassandraConnector.getInstance().getSession().getMetadata().getNodes().values();
            String msg = objectMapper.writeValueAsString(Collections.singletonMap("status", nodes.stream().anyMatch(n -> n.getState() == NodeState.UP)));
            exchange.sendResponseHeaders(200, msg.length());
            OutputStream os = exchange.getResponseBody();
            os.write(msg.getBytes(StandardCharsets.UTF_8));
            os.close();
        });

        if ("true".equals(System.getenv("ENABLE_VIRTUAL_THREADS"))) {
            log.info("Enabled Virtual Threads");
            httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        }
        httpServer.start();
    }

}
