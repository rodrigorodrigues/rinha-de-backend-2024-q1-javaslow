FROM container-registry.oracle.com/os/oraclelinux:8-slim
COPY target/rinha-backend2024-q1-javaslow javaslow
ENTRYPOINT ["/javaslow"]