package com.example.dto;

import java.time.Instant;

public record Balance(Integer total, Instant instant,
                      Integer creditLimit) {
    @Override
    public String toString() {
        return "{" +
                "\"total\":" + total +
                ",\"data_extrato\":\"" + instant +
                "\",\"limite\":" + creditLimit +
                "}";
    }
}
