package com.example;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer httpServer = HttpServer
                .create(new InetSocketAddress(Integer.parseInt(System.getenv("SERVER_PORT"))), 0);

        httpServer.createContext("/clientes", new CreateTransactionHandler(new AccountRepository(), new TransactionRepository()));

        if ("true".equals(System.getenv("ENABLE_VIRTUAL_THREADS"))) {
            httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        }
        httpServer.start();
    }

}
