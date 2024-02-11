package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TransactionResponse(@JsonProperty("limite") Integer creditLimit, @JsonProperty("saldo") Integer balance) {
}
