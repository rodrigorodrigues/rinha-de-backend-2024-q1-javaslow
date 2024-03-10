package com.example;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;

import java.net.InetSocketAddress;

public class CassandraConnector {
    private static final CassandraConnector INSTANCE = new CassandraConnector();
    private final Cluster cluster;

    private CassandraConnector() {
        var cassandraHost = System.getenv("CASSANDRA_HOST");
        var cassandraPort = Integer.parseInt(System.getenv("CASSANDRA_PORT"));
        this.cluster = cluster(cassandraHost, cassandraPort);
    }

    private Cluster cluster(String cassandraHost, int cassandraPort) {
        var poolingOptions = new PoolingOptions();
        poolingOptions.setMaxRequestsPerConnection(HostDistance.LOCAL, 32768);
        poolingOptions.setMaxRequestsPerConnection(HostDistance.REMOTE, 2000);

        var builder = Cluster.builder();
        builder.addContactPointsWithPorts(new InetSocketAddress(cassandraHost, cassandraPort));
        builder.withPoolingOptions(poolingOptions);

        return builder.build();
    }

    public static CassandraConnector getInstance() {
        return INSTANCE;
    }

    public Cluster getCluster() {
        return this.cluster;
    }
}
