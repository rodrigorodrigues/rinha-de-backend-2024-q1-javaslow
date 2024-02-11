package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record Balance(Integer total, @JsonProperty("data_extrato") Instant instant,
                      @JsonProperty("limite") Integer creditLimit) {
}
