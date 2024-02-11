package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public class CreateTransactionHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(CreateTransactionHandler.class.getName());

    private final ValidatorFactory factory = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory();
    private final Validator validator = factory.getValidator();
    private final AccountRepository accountRepository;

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .disable(JsonGenerator.Feature.IGNORE_UNKNOWN)
            .registerModule(new JavaTimeModule());

    public CreateTransactionHandler(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String id = path.substring(path.indexOf("clientes") + 9, path.lastIndexOf("/"));

            int accountId;
            try {
                accountId = Integer.parseInt(id);
            } catch (NumberFormatException nfe) {
                response(exchange, HttpURLConnection.HTTP_BAD_GATEWAY, "Not valid path parameter");
                return;
            }

            var account = accountRepository.findById(accountId);
            if (account == null) {
                String msg = String.format("Not found account id: %s", id);
                response(exchange, HttpURLConnection.HTTP_NOT_FOUND, msg);
                return;
            }

            if (exchange.getRequestMethod().equals("GET") && path.endsWith("extrato")) {

                List<Transaction> transactions = transactionRepository.findLastTransactionsByAccountId(accountId);
                var total = transactions.stream().mapToInt(Transaction::amount).sum();

                String json = objectMapper.writeValueAsString(new AccountTransactionsResponse(new Balance(total, Instant.now(), account.creditLimit()), transactions));

                exchange.getResponseHeaders().add("Content-type", "application/json");
                response(exchange, HttpURLConnection.HTTP_OK, json);

            } else if (exchange.getRequestMethod().equals("POST") && path.endsWith("transacoes")) {
                TransactionRequest transactionRequest = objectMapper.readValue(exchange.getRequestBody(), TransactionRequest.class);

                if (validateRequest(exchange, transactionRequest)) return;

                saveTransaction(exchange, account, transactionRequest);
            } else {
                throw new IllegalStateException("Resource not available!");
            }
        } catch (Exception e) {
            log.error(String.format("Unexpected error = %s", e.getLocalizedMessage()), e);
            response(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, String.format("Unexpected error: %s", e.getLocalizedMessage()));
        }
    }

    private boolean validateRequest(HttpExchange exchange, TransactionRequest transactionRequest) throws IOException {
        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(transactionRequest);
        if (!violations.isEmpty()) {
            StringBuilder error = new StringBuilder();
            for (ConstraintViolation<TransactionRequest> violation : violations) {
                error.append(violation.getPropertyPath())
                        .append(": ")
                        .append(violation.getMessage())
                        .append(System.getProperty("line.separator"));
            }
            response(exchange, HttpURLConnection.HTTP_BAD_REQUEST, error.toString());
            return true;
        }
        return false;
    }

    private void saveTransaction(HttpExchange exchange, Account account, TransactionRequest transactionRequest) throws IOException {
        var balance = account.balance();
        if (transactionRequest.type().equals("c")) {
            balance += transactionRequest.amount();
        } else {
            balance -= transactionRequest.amount();
            if (account.creditLimit() + balance < 0) {
                log.warn("Balance is lower than allowed");
                response(exchange, 422, "Balance is lower than allowed");
                return;
            }
        }
        accountRepository.updateBalance(account.withBalance(balance));
        transactionRepository.save(new Transaction(account.id(), transactionRequest.type(), transactionRequest.description(), Instant.now(), transactionRequest.amount()));

        String json = objectMapper.writeValueAsString(new TransactionResponse(account.creditLimit(), account.balance()));
        exchange.getResponseHeaders().add("Content-type", "application/json");
        response(exchange, HttpURLConnection.HTTP_OK, json);
    }

    private void response(HttpExchange exchange, int statusCode, String msg) throws IOException {
        if (statusCode != 200) {
            log.warn(String.format( "status = {%s}={%s} ", statusCode, msg));
        }
        exchange.sendResponseHeaders(statusCode, msg.length());
        OutputStream os = exchange.getResponseBody();
        os.write(msg.getBytes(StandardCharsets.UTF_8));
        os.close();
    }

    public record TransactionRequest(@NotNull @Min(0) @JsonProperty("valor") Integer amount, @NotNull @Pattern(regexp = "[c|d]") @JsonProperty("tipo") String type,
                                     @NotBlank @Length(max = 10) @JsonProperty("descricao") String description) {
    }

    public record TransactionResponse(@JsonProperty("limite") Integer creditLimit, @JsonProperty("saldo") Integer balance) {
    }

    public record AccountTransactionsResponse(@JsonProperty("saldo") Balance balance, @JsonProperty("ultimas_transacoes") List<Transaction> lastTransactions) {
    }

    public record Balance(Integer total, @JsonProperty("data_extrato") Instant instant,
                          @JsonProperty("limite") Integer creditLimit) {
    }
}