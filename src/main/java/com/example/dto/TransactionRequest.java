package com.example.dto;

public record TransactionRequest(Integer amount, String type, String description) {
}
