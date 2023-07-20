package testcontainers

import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer

object InteractWithDatabase
    extends ZIOAppDefault:
//  val logic =
//    for {
//      _ <-
//
//    }

  def run =
    UserService
      .get("blah")
      .provide(
        UserServiceLive.layer,
//      UserActionServiceLive.layer,
        QuillContext.dataSourceLayer
      )
