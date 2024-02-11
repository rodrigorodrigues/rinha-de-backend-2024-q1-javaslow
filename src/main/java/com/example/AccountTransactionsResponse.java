package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AccountTransactionsResponse(@JsonProperty("saldo") Balance balance, @JsonProperty("ultimas_transacoes") List<Transaction> lastTransactions) {
}