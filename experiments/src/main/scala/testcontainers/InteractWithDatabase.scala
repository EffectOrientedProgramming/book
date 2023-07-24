package testcontainers

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
