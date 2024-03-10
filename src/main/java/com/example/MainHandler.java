package com.example;

import com.example.dto.AccountTransactionsResponse;
import com.example.dto.Balance;
import com.example.dto.TransactionRequest;
import com.example.dto.TransactionResponse;
import com.example.model.Transaction;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class MainHandler implements HttpHandler {

    private final MainRepository mainRepository;

    private final Pattern pattern = Pattern.compile("^[c|d]");
    private final Pattern removeQuotes = Pattern.compile("\"");
    private final Map<Integer, ExecutorService> executors = Map.ofEntries(
            new AbstractMap.SimpleEntry<>(1, Executors.newSingleThreadExecutor(new DefaultThreadFactory("client 1"))),
            new AbstractMap.SimpleEntry<>(2, Executors.newSingleThreadExecutor(new DefaultThreadFactory("client 2"))),
            new AbstractMap.SimpleEntry<>(3, Executors.newSingleThreadExecutor(new DefaultThreadFactory("client 3"))),
            new AbstractMap.SimpleEntry<>(4, Executors.newSingleThreadExecutor(new DefaultThreadFactory("client 4"))),
            new AbstractMap.SimpleEntry<>(5, Executors.newSingleThreadExecutor(new DefaultThreadFactory("client 5")))
    );

    public MainHandler(MainRepository mainRepository) {
        this.mainRepository = mainRepository;
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

            var creditLimit = mainRepository.getCreditLimitByAccountId(accountId);
            if (creditLimit == null) {
                String msg = String.format("Not found account id: %s", id);
                response(exchange, HttpURLConnection.HTTP_NOT_FOUND, msg);
                return;
            }

            if (exchange.getRequestMethod().equals("GET") && path.endsWith("extrato")) {
                List<Transaction> transactions = mainRepository.findLastTransactionsByAccountId(accountId);
                var total = mainRepository.totalBalanceByAccountId(accountId);

                var json = new AccountTransactionsResponse(new Balance(total, Instant.now(), creditLimit), transactions).toString();

                exchange.getResponseHeaders().add("Content-type", "application/json");
                response(exchange, HttpURLConnection.HTTP_OK, json);

            } else if (exchange.getRequestMethod().equals("POST") && path.endsWith("transacoes")) {
                TransactionRequest transactionRequest = validateRequest(exchange);

                if (transactionRequest == null) {
                    response(exchange, 422, "invalid data");
                    return;
                }

                try {
                    executors.get(accountId).submit(() -> processRequest(exchange, accountId, creditLimit, transactionRequest))
                            .get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    response(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, String.format("Unexpected error: %s", e.getLocalizedMessage()));
                }

            } else {
                throw new IllegalStateException("Resource not available!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, String.format("Unexpected error: %s", e.getLocalizedMessage()));
        }
    }

    private TransactionRequest validateRequest(HttpExchange exchange) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null ) {
                sb.append(line);
            }
            String json = sb.toString();
            if (!json.startsWith("{") || !json.endsWith("}")) {
                System.out.printf("invalidJson: Invalid data: %s\n", json);
                return null;
            }
            String[] tokens = removeQuotes.matcher(json.substring(1, json.length() - 1))
                    .replaceAll("")
                    .split(",");
            int amount = 0;
            String type = null;
            String description = null;
            for (String token : tokens) {
                token = token.trim();
                if (!token.startsWith("valor") && !token.startsWith("tipo") && !token.startsWith("descricao")) {
                    System.out.printf("invalidName: Invalid data: %s\n", json);
                    return null;
                }
                String[] split = token.split(":");
                if (split.length != 2) {
                    System.out.printf("invalidSyntax:Invalid data: %s\n", json);
                    return null;
                }
                String key = split[0].trim();
                String value = split[1].trim();
                if (key.equals("valor")) {
                    try {
                        amount = Integer.parseInt(value);
                        if (amount < 0) {
                            System.out.printf("invalidAmount: Invalid data: %s\n", json);
                            return null;
                        }
                    } catch (NumberFormatException nfe) {
                        System.out.printf("numberFormat: Invalid data: %s\n", json);
                        return null;
                    }
                } else if (key.equals("tipo") && pattern.matcher(value).matches()) {
                    type = value;
                } else if (key.equals("descricao") && value.length() <= 10) {
                    description = value;
                } else {
                    System.out.printf("noelse: Invalid data: %s\n", json);
                    return null;
                }
            }

            return new TransactionRequest(amount, type, description);
        }
    }

    private void processRequest(HttpExchange exchange, Integer accountId, Integer creditLimit, TransactionRequest transactionRequest) {
        try {
            var total = mainRepository.totalBalanceByAccountId(accountId);
            var amount = transactionRequest.amount();

            if (transactionRequest.type().equals("d")) {
                if ((amount + Math.abs(total)) > creditLimit) {
                    response(exchange, 422, "Balance is lower than allowed");
                    return;
                }
                amount = -amount;
            }

            mainRepository.updateBalance(amount, accountId);
            mainRepository.saveTransaction(new Transaction(accountId, transactionRequest.type(), transactionRequest.description(), Instant.now(), transactionRequest.amount()));
            var json = new TransactionResponse(creditLimit, total + amount).toString();
            exchange.getResponseHeaders().add("Content-type", "application/json");
            response(exchange, HttpURLConnection.HTTP_OK, json);
        } catch (IOException ioe) {
            throw new RuntimeException("Unexpected io error", ioe);
        }
    }

    private void response(HttpExchange exchange, int statusCode, String msg) throws IOException {
        if (statusCode != 200) {
            System.out.printf("Error: statusCode: %s\tmsg: %s%n", statusCode, msg);
        }
        exchange.sendResponseHeaders(statusCode, msg.length());
        OutputStream os = exchange.getResponseBody();
        os.write(msg.getBytes(StandardCharsets.UTF_8));
        os.close();
    }

}