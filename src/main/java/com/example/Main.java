package com.example;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws IOException {
        Instant startTime = Instant.now();
        int serverPort = Integer.parseInt(System.getenv("SERVER_PORT"));
        HttpServer httpServer = HttpServer
                .create(new InetSocketAddress(serverPort), 0);

        httpServer.createContext("/clientes", new MainHandler(new MainRepository()));
        httpServer.createContext("/healthcheck", exchange -> {
            String msg = String.format("{\"cassandra_up\":\"%s\"}", !CassandraConnector.getInstance().getCluster().isClosed());
            exchange.getResponseHeaders().add("Content-type", "application/json");
            exchange.sendResponseHeaders(200, msg.length());
            OutputStream os = exchange.getResponseBody();
            os.write(msg.getBytes(StandardCharsets.UTF_8));
            os.close();
        });

        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        httpServer.start();
        System.out.printf("Started on port: %s in %s millis.\n", serverPort, Duration.between(startTime, Instant.now()).toMillis());
    }

}
