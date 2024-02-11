### Rinha de Backend 2024 Q1 - Java eh lento

### Stack

* Java 21 no **Framework**
* GraalVM
* Cassandra

### Installation with GraalVM

```
mvn clean package -Pnative

docker build -t rinha-de-backend-2024-q1-javaslow .
```

### Installation without GraalVM

`mvn clean package jib:dockerBuild`


### Run locally

`SERVER_PORT=9999 CASSANDRA_PORT=9042 CASSANDRA_HOST=localhost CASSANDRA_DC=datacenter1 ./target/rinha-backend2024-q1-javaslow`