package com.example.dto;

import com.example.model.Transaction;

import java.util.List;

public record AccountTransactionsResponse(Balance balance, List<Transaction> lastTransactions) {
    @Override
    public String toString() {
        return "{" +
                "\"saldo\":" + balance +
                ",\"ultimas_transacoes\":" + lastTransactions +
                "}";
    }
}
