package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record Account(Integer id, @NotNull @JsonProperty("limite") Integer creditLimit, @NotNull @JsonProperty("saldo") Integer balance) {
        public Account withBalance(Integer balance) {
                return new Account(id, creditLimit, balance);
        }
}
