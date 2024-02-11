package com.example;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.insert.RegularInsert;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.*;

public class TransactionRepository {
    private final CqlSession session = CassandraConnector.getInstance().getSession();

    public boolean save(Transaction transaction) {
        RegularInsert insertInto = QueryBuilder.insertInto("transactions")
                .value("accountId", bindMarker())
                .value("type", bindMarker())
                .value("description", bindMarker())
                .value("date", bindMarker())
                .value("amount", bindMarker());

        SimpleStatement insertStatement = insertInto.build();

        PreparedStatement preparedStatement = session.prepare(insertStatement);

        BoundStatement statement = preparedStatement.bind()
                .setInt(0, transaction.accountId())
                .setString(1, transaction.type())
                .setString(2, transaction.description())
                .setInstant(3, Instant.now())
                .setInt(4, transaction.amount());

        return session.execute(statement).wasApplied();
    }

    public List<Transaction> findLastTransactionsByAccountId(Integer accountId) {
        var select = selectFrom("transactions")
                .all()
                .whereColumn("accountId")
                .isEqualTo(literal(accountId))
                .limit(10)
                .build();
        List<Row> all = session.execute(select).all();
        if (all == null || all.isEmpty()) {
            return Collections.emptyList();
        }
        return all.stream()
                .map(t -> new Transaction(t.getInt("accountId"), t.getString("type"), t.getString("description"), t.getInstant("date"), t.getInt("amount")))
                .collect(Collectors.toList());
    }

}
