package com.example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;

public record Transaction(@JsonIgnore @NotNull Integer accountId,
                          @NotBlank @JsonProperty("tipo") String type,
                          @NotBlank @Length(max = 10) @JsonProperty("descricao") String description,
                          @NotNull @JsonProperty("realizada_em") Instant date,
                          @NotNull @JsonProperty("valor") Integer amount) {
}
