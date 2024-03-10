package com.example;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Insert;
import com.example.model.Transaction;

import java.sql.Date;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

public class MainRepository {
    private final Cluster cluster = CassandraConnector.getInstance().getCluster();
    private final Map<Integer, Integer> accounts = Map.ofEntries(
            new AbstractMap.SimpleEntry<>(1, 100000),
            new AbstractMap.SimpleEntry<>(2, 80000),
            new AbstractMap.SimpleEntry<>(3, 1000000),
            new AbstractMap.SimpleEntry<>(4, 10000000),
            new AbstractMap.SimpleEntry<>(5, 500000)
    );

    public Integer getCreditLimitByAccountId(Integer id) {
        return accounts.get(id);
    }

    public void updateBalance(Integer amount, Integer id) {
        try (var session = cluster.connect()) {
            var update = update("rinha", "accounts_balance")
                    .with(incr("total", amount))
                    .where(eq("accountId", id));

            session.execute(update);
        }
    }

    public Integer totalBalanceByAccountId(Integer id) {
        try (var session = cluster.connect()) {
            var select = select("total")
                    .from("rinha", "accounts_balance")
                    .where(eq("accountId", id));
            Row row = session.execute(select).one();
            if (row == null) {
                return null;
            }
            return (int) row.getLong("total");
        }
    }

    public void saveTransaction(Transaction transaction) {
        try (var session = cluster.connect()) {
            Insert insert = insertInto("rinha", "transactions")
                    .value("accountId", bindMarker())
                    .value("type", bindMarker())
                    .value("description", bindMarker())
                    .value("date", bindMarker())
                    .value("amount", bindMarker())
                    .value("dateMillis", bindMarker());

            PreparedStatement preparedStatement = session.prepare(insert);

            Instant now = Instant.now();
            BoundStatement saveTransaction = preparedStatement.bind()
                    .setInt("accountId", transaction.accountId())
                    .setString("type", transaction.type())
                    .setString("description", transaction.description())
                    .setDate("date", Date.from(now))
                    .setInt("amount", transaction.amount())
                    .setLong("dateMillis", now.toEpochMilli());

            session.execute(saveTransaction);
        }
    }

    public List<Transaction> findLastTransactionsByAccountId(Integer accountId) {
        try (var session = cluster.connect()) {
            var select = select()
                    .from("rinha", "transactions")
                    .where(eq("accountId", accountId))
                    .limit(10);
            List<Row> all = session.execute(select).all();
            if (all.isEmpty()) {
                return Collections.emptyList();
            }
            return all.stream()
                    .map(t -> new Transaction(t.getInt("accountId"), t.getString("type"), t.getString("description"), t.getDate("date").toInstant(), t.getInt("amount")))
                    .collect(Collectors.toList());
        }
    }
}
