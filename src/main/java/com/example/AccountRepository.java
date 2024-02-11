package com.example;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.*;

public class AccountRepository {
    private final CqlSession session = CassandraConnector.getInstance().getSession();

    public boolean updateBalance(Account account) {
        SimpleStatement statement = update("accounts")
                .setColumn("balance", literal(account.balance()))
                .whereColumn("id").isEqualTo(literal(account.id()))
                .build();
        return session.execute(statement).wasApplied();
    }

    public Account findById(Integer id) {
        var select = selectFrom("accounts")
                .all()
                .whereColumn("id").isEqualTo(literal(id))
                .build();
        Row row = session.execute(select).one();
        if (row == null) {
            return null;
        }
        return new Account(id, row.getInt("creditLimit"), row.getInt("balance"));
    }
}
