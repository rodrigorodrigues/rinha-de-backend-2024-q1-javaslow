DROP KEYSPACE IF EXISTS rinha;

CREATE KEYSPACE IF NOT EXISTS rinha WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE rinha.accounts_balance (
    accountId int,
    total counter,
    PRIMARY KEY (accountId)
);

CREATE TABLE IF NOT EXISTS rinha.transactions (
    accountId int,
    type text,
    description text,
    date timestamp,
    amount int,
    dateMillis bigint,
    PRIMARY KEY ((accountId), dateMillis)
) WITH CLUSTERING ORDER BY (dateMillis DESC);

UPDATE rinha.accounts_balance SET total = total + 0 WHERE accountId = 1;
UPDATE rinha.accounts_balance SET total = total + 0 WHERE accountId = 2;
UPDATE rinha.accounts_balance SET total = total + 0 WHERE accountId = 3;
UPDATE rinha.accounts_balance SET total = total + 0 WHERE accountId = 4;
UPDATE rinha.accounts_balance SET total = total + 0 WHERE accountId = 5;