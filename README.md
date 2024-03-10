### Rinha de Backend 2024 Q1 - Java eh lento

### Stack

* Java 21 without **Framework**
* GraalVM
* Cassandra

### GraalVM Instrumented

```
mvn clean package -Pnative-instrumented
```

### GraalVM Optimized

```
mvn clean package -Pnative-optimized
```

### Docker Image

```
docker build -t rinha-de-backend-2024-q1-javaslow .
```

### Docker Compose

```
docker-compose up -d
```

### Run locally

`SERVER_PORT=9999 CASSANDRA_PORT=9042 CASSANDRA_HOST=localhost CASSANDRA_DC=datacenter1 ENABLE_VIRTUAL_THREADS=true ./target/rinha-backend2024-q1-javaslow`