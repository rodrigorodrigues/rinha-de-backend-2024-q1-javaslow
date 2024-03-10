package com.example.dto;

public record TransactionResponse(Integer creditLimit, Integer balance) {
    @Override
    public String toString() {
        return "{" +
                "\"limite\":" + creditLimit +
                ",\"saldo\":" + balance +
                "}";
    }
}
