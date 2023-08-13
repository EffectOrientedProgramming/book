package environment

object DatabaseConnection extends ZIOAppDefault:
  opaque type UserId = String
  case class DbConnection(
      actionLog: Ref[Seq[String]]
  ):
    def execute(label: String) =
      actionLog.update(_.appended(label))

  object DbConnection:
    val make
        : ZLayer[Any, Nothing, DbConnection] =
      ZLayer.scoped(
        ZIO.acquireRelease(
          defer {
            val actionLog: Ref[Seq[String]] =
              Ref.make(Seq.empty[String]).run
            val dbConnection =
              DbConnection(actionLog)
            dbConnection.execute("OPEN").run
            dbConnection
          }
        )(dbConnection =>
          defer {
            dbConnection.execute("CLOSE").run
            pprint.apply(dbConnection)
          }.debug
        )
      )
  end DbConnection

  def executeUserQueries(
      userId: UserId
  ): ZIO[DbConnection, Nothing, Unit] =
    defer {
      val dbConnection =
        ZIO.service[DbConnection].run
      dbConnection
        .execute(
          s"QUERY for $userId preferences"
            .stripMargin
        )
        .run
      ZIO.sleep(1.second).run
      dbConnection
        .execute(
          s"QUERY for $userId accountHistory"
            .stripMargin
        )
        .run
    }

  def run =
    defer {
      executeUserQueries("Alice").run
      executeUserQueries("Bob").run
    }.provide(DbConnection.make)
end DatabaseConnection
