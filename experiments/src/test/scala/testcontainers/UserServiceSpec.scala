package testcontainers

object UserServiceSpec
/* extends ZIOSpec[DataSource & JdbcInfo]:
 * val bootstrap = SharedDbLayer.layer def spec =
 * suite("UserService")( test("retrieves an
 * existin user")( for user <- UserService
 * .get("uuid_hard_coded") .debug yield
 * assertCompletes ), test("inserts a user") {
 * val newUser =
 * User("user_id_from_app", "Appy") for _ <-
 * UserService.insert(newUser) user <-
 * UserService.get(newUser.userId) yield
 * assertTrue(newUser == user) }
 * ).provideSomeShared[DataSource](
 * UserServiceLive.layer ) end UserServiceSpec */
