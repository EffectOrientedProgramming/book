package environment

opaque type UserId =
  String
object UserId:
  def apply(str: String): UserId =
    str

case class DbConnection(
    actionLog: Ref[Seq[String]]
):
  def execute(label: String) =
    actionLog.update(_.appended(label))

object DbConnection:
  val make: ZLayer[Any, Nothing, DbConnection] =
    ZLayer.scoped(
      ZIO.acquireRelease(
        defer:
          val actionLog =
            Ref.make(Seq.empty[String]).run
          val connection =
            DbConnection(actionLog)
          connection.execute("OPEN").run
          connection
      )(connection =>
        defer:
          connection.execute("CLOSE").run
          pprint.apply(connection)
        .debug
      )
    )

object DatabaseConnectionSimple
    extends ZIOAppDefault:

  def executeUserQueries(
      userId: UserId
  ): ZIO[DbConnection, Nothing, Unit] =
    // TODO Consider alternative version of this
    // where the defer happens inside of a
    // serviceWithZIO call
    defer {
      val connection =
        ZIO.service[DbConnection].run
      connection
        .execute(
          s"QUERY for $userId preferences"
            .stripMargin
        )
        .run
      ZIO.sleep(1.second).run
      connection
        .execute(
          s"QUERY for $userId accountHistory"
            .stripMargin
        )
        .run
    }

  def run =
    defer {
      executeUserQueries(UserId("Alice")).run
      executeUserQueries(UserId("Bob")).run
    }.provide(DbConnection.make)
end DatabaseConnectionSimple

object DatabaseConnectionInterleavedQueries
    extends ZIOAppDefault:

  def executeUserQueries(
      userId: UserId
  ): ZIO[DbConnection, Nothing, Unit] =
    // TODO Consider alternative version of this
    // where the defer happens inside of a
    // serviceWithZIO call
    defer {
      val connection =
        ZIO.service[DbConnection].run
      connection
        .execute(
          s"QUERY for $userId preferences"
            .stripMargin
        )
        .run
      ZIO.sleep(1.second).run
      connection
        .execute(
          s"QUERY for $userId accountHistory"
            .stripMargin
        )
        .run
    }

  def run =
    ZIO
      .foreachPar(
        List(UserId("Alice"), UserId("Bob"))
      )(executeUserQueries)
      .provide(DbConnection.make)
end DatabaseConnectionInterleavedQueries
