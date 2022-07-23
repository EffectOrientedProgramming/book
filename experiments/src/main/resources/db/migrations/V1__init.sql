CREATE TABLE MyFirstTable(
                             id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                             uKey VARCHAR(200) NOT NULL,
                             uValue TEXT NOT NULL,
                             createdAt TIMESTAMP NOT NULL,
                             updatedAt TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX MyFirstTable__Key
    ON MyFirstTable(uKey);
