CREATE TABLE user_action(
                             user_id TEXT,
                             action_type TEXT NOT NULL,
                             timestamp TEXT,
                             CONSTRAINT fk_user_id
                                 FOREIGN KEY(user_id)
                                     REFERENCES app_user(user_id)
--                              uKey VARCHAR(200) NOT NULL,
);

-- CREATE UNIQUE INDEX MyFirstTable__Key
--     ON MyFirstTable(uKey);

INSERT INTO user_action(
                  user_id,
                        action_type,
                        timestamp

) VALUES
      (
          'uuid_hard_coded',
          'LogIn',
          '2021-03-24T16:48:05.591+08:00'
      ),(
          'uuid_hard_coded',
          'UpdatePreferences',
          '2021-03-24T16:48:05.591+08:00'
      ),(
          'uuid_hard_coded',
          'LogOut',
          '2021-03-24T16:48:05.591+08:00'
      );