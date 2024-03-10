#FROM  eclipse-temurin:21.0.2_13-jre-alpine
#FROM  eclipse-temurin:21.0.2_13-jre-alpine
#FROM gcr.io/distroless/base
FROM ubuntu:jammy
COPY target/rinha-backend2024-q1-javaslow javaslow
ENTRYPOINT ["/javaslow"]