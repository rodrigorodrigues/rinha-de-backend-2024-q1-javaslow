package com.example;

import com.datastax.oss.driver.api.core.CqlSession;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class CassandraConnector {
    private static final Logger log = Logger.getLogger(CassandraConnector.class.getName());
    private static final CassandraConnector INSTANCE = new CassandraConnector();
    private CqlSession session;

    private CassandraConnector() {
        var cassandraHost = System.getenv("CASSANDRA_HOST");
        var port = Integer.parseInt(System.getenv("CASSANDRA_PORT"));
        var cassandraDc = System.getenv("CASSANDRA_DC");
        connect(cassandraHost, port, cassandraDc);
    }

    private void connect(String node, Integer port, String dataCenter) {
        log.info(String.format("Cassandra node: %s", node));
        log.info(String.format("Cassandra port: %s", port));
        log.info(String.format("Cassandra dataCenter: %s", dataCenter));
        var builder = CqlSession.builder();
        builder.addContactPoint(new InetSocketAddress(node, port));
        builder.withLocalDatacenter(dataCenter);
        builder.withKeyspace("rinha");

        session = builder.build();
    }

    public static CassandraConnector getInstance() {
        return INSTANCE;
    }

    public CqlSession getSession() {
        return this.session;
    }
}
