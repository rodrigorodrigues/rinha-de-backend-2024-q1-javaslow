package com.example;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Testcontainers
class MainTest {

    @Container
    private static final CassandraContainer<?> cassandraContainer = new CassandraContainer<>("cassandra:latest")
            .withExposedPorts(9042)
            .withInitScript("schema.cql");

    @BeforeEach
    void setup() throws Exception {
        setEnv("CASSANDRA_HOST", cassandraContainer.getHost());
        setEnv("CASSANDRA_PORT", cassandraContainer.getMappedPort(9042)+"");
        setEnv("CASSANDRA_DC", cassandraContainer.getLocalDatacenter());
        setEnv("SERVER_PORT", "8080");
    }

    private void setEnv(String key, String val) throws Exception {
        getModifiableEnv().put(key, val);
    }

    private Map<String, String> getModifiableEnv() throws Exception {
        Map<String, String> unmodifiableEnv = System.getenv();
        Field field = unmodifiableEnv.getClass().getDeclaredField("m");
        field.setAccessible(true);
        return (Map<String, String>) field.get(unmodifiableEnv);
    }

    @Test
    void testMain() throws IOException {
        Main.main(new String[]{});

        given().get("http://localhost:8080/healthcheck")
                .then()
                .statusCode(200)
                .body("cassandra_up", equalTo("true"))
                .log().all();

        given()
                .contentType(ContentType.JSON)
                .body("{\"valor\": 10, \"tipo\": \"c\", \"descricao\": \"danada\"}")
                .when()
                .post("http://localhost:8080/clientes/5/transacoes")
                .then()
                .statusCode(200)
                .log().all();

        given()
                .contentType(ContentType.JSON)
                .body("{\"valor\": 500001, \"tipo\": \"d\", \"descricao\": \"danada\"}")
                .when()
                .post("http://localhost:8080/clientes/5/transacoes")
                .then()
                .statusCode(422)
                .log().all();

        given()
                .contentType(ContentType.JSON)
                .body("{\"valor\": -10, \"tipo\": \"a\", \"descricao\": null}")
                .when()
                .post("http://localhost:8080/clientes/5/transacoes")
                .then()
                .statusCode(422)
                .log().all();

        given().get("http://localhost:8080/clientes/5/extrato")
                .then()
                .statusCode(200)
                .body("saldo.total", equalTo(10))
                .body("saldo.limite", notNullValue())
                .body("ultimas_transacoes[0].tipo", equalTo("c"))
                .body("ultimas_transacoes[0].descricao", equalTo("danada"))
                .body("ultimas_transacoes[0].realizada_em", notNullValue())
                .body("ultimas_transacoes[0].valor", equalTo(10))
                .log().all();

    }
}