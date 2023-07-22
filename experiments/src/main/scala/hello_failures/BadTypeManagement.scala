package hello_failures

object BadTypeManagement
    extends zio.ZIOAppDefault:
  val logic: ZIO[Any, Exception, String] =
    defer {
      ZIO.debug("ah").run
      val result =
        failable(1)
          .catchAll {
            case ex: Exception =>
              ZIO.fail(ex)
            case ex: String =>
              ZIO.succeed(
                "recovered string error: " + ex
              )
          }
          .run
      ZIO.debug(result).run
      result
    }
  def run = logic

  def failable(
      path: Int
  ): ZIO[Any, Exception | String, String] =
    if (path < 0)
      ZIO.fail(new Exception("Negative path"))
    else if (path > 0)
      ZIO.fail("Too big")
    else
      ZIO.succeed("just right")
end BadTypeManagement
