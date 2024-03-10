package com.example.model;

import java.time.Instant;

public record Transaction(Integer accountId,
                          String type,
                          String description,
                          Instant date,
                          Integer amount) {
    @Override
    public String toString() {
        return "{" +
                "\"tipo\":\"" + type +
                "\",\"descricao\":\"" + description +
                "\",\"realizada_em\":\"" + date +
                "\",\"valor\":" + amount +
                "}";
    }
}
