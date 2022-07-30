CREATE TABLE app_user(
                             user_id TEXT NOT NULL PRIMARY KEY,
                             name TEXT NOT NULL
--                              uKey VARCHAR(200) NOT NULL,
);

-- CREATE UNIQUE INDEX MyFirstTable__Key
--     ON MyFirstTable(uKey);

INSERT INTO app_user(
          user_id, name

) VALUES  (
           'uuid_hard_coded',
           'Bill'
          );