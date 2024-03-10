package com.example;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.insert.RegularInsert;
import com.example.model.Transaction;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.*;

public class MainRepository {
    private final CqlSession session = CassandraConnector.getInstance().getSession();
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
        SimpleStatement update = update("accounts_balance")
                .increment("total", literal(amount))
                .whereColumn("accountId").isEqualTo(literal(id))
                .build();

        session.execute(update);
    }

    public Integer totalBalanceByAccountId(Integer id) {
        var select = selectFrom("accounts_balance")
                .columns("total")
                .whereColumn("accountId").isEqualTo(literal(id))
                .build();
        Row row = session.execute(select).one();
        if (row == null) {
            return null;
        }
        return (int) row.getLong("total");
    }

    public void saveTransaction(Transaction transaction) {
        RegularInsert insertInto = QueryBuilder.insertInto("transactions")
                .value("accountId", bindMarker())
                .value("type", bindMarker())
                .value("description", bindMarker())
                .value("date", bindMarker())
                .value("amount", bindMarker())
                .value("dateMillis", bindMarker());

        SimpleStatement insertStatement = insertInto.build();

        PreparedStatement preparedStatement = session.prepare(insertStatement);

        Instant now = Instant.now();
        BoundStatement saveTransaction = preparedStatement.bind()
                .setInt("accountId", transaction.accountId())
                .setString("type", transaction.type())
                .setString("description", transaction.description())
                .setInstant("date", now)
                .setInt("amount", transaction.amount())
                .setLong("dateMillis", now.toEpochMilli());

        session.execute(saveTransaction);
    }

    public List<Transaction> findLastTransactionsByAccountId(Integer accountId) {
        var select = selectFrom("transactions")
                .all()
                .whereColumn("accountId")
                .isEqualTo(literal(accountId))
                .limit(10)
                .build();
        List<Row> all = session.execute(select).all();
        if (all.isEmpty()) {
            return Collections.emptyList();
        }
        return all.stream()
                .map(t -> new Transaction(t.getInt("accountId"), t.getString("type"), t.getString("description"), t.getInstant("date"), t.getInt("amount")))
                .collect(Collectors.toList());
    }
}
