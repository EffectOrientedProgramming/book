package mdoc

import zio.*
import zio.Console.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.environment.*

import java.io.IOException

import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.{
  GenericContainer,
  Network
}

import io.getquill._

object MyApp:
  def quillStuff(
      exposedPort: Int,
      username: String,
      password: String
  ) =
    case class Person(
        firstName: String,
        lastName: String,
        age: Int
    )

    import com.zaxxer.hikari.{
      HikariConfig,
      HikariDataSource
    }
    val pgDataSource =
      new org.postgresql.ds.PGSimpleDataSource()
    pgDataSource.setUser(username)
    pgDataSource
      .setPortNumbers(Array(exposedPort))
    pgDataSource.setPassword(password)
    val config = new HikariConfig()
    config.setDataSource(pgDataSource)
    val ctx =
      new PostgresJdbcContext(
        LowerCase,
        new HikariDataSource(config)
      )

    // SnakeCase turns firstName -> first_name
    // val ctx = new
    // PostgresJdbcContext(SnakeCase, "")
    import ctx._

    val named = "Joe"
    inline def somePeople =
      quote {
        query[Person].filter(p =>
          p.firstName == lift(named)
        )
      }
    val people: List[Person] = run(somePeople)
    // TODO Get SQL
    println(people)
  end quillStuff
end MyApp

object ManagedTestInstances:
    

end ManagedTestInstances

// TODO Figure out if TESTCONTAINERS_RYUK_DISABLED=true is a band-aid that's avoiding the real problem with test cleanup

object TestContainersSpec
    extends DefaultRunnableSpec:

  def spec =
    suite("mdoc.MdocHelperSpec")(
      test(
        "Intercept and format MatchError from unhandled RuntimeException"
      ) {
        for
          _ <- 
            ZIO
              .acquireReleaseWith(
                ZIO.succeed(Network.newNetwork().nn), (n: Network) =>  ZIO.debug("Trying to close network..."), {

                  (network: Network) => 
                    for {
                      safePostgres <-
                        PostgresContainer
                          .construct("init.sql")
                          .provideSomeLayer(
                            ZLayer.succeed(network)
                          )
                      _ <- ZIO.succeed(safePostgres.start)
                      _ <-
                        mdoc.wrapUnsafeZIO(
                          ZIO.succeed(
                            throw new MatchError(
                              MdocSession.App.GpsException()
                            )
                          )
                        )
                      _ <-
                        ZIO.attempt {
                          println(
                            "Bound port numbers: " +
                              safePostgres
                                .getMappedPort(5432)
                          )
                          println(
                            "Exposed ports: " +
                              safePostgres.getExposedPorts
                          )
                          MyApp.quillStuff(
                            safePostgres
                              .getMappedPort(5432)
                              .nn,
                            safePostgres.getUsername.nn,
                            safePostgres.getPassword.nn
                          )
                        }
                      _ <- ZIO.succeed(safePostgres.close)
                    } yield ()
                })
          output <- TestConsole.output
        yield assert(output)(
          equalTo(
            Vector(
              "Defect: class scala.MatchError\n",
              "        GpsException\n"
            )
          )
        )
      }
    )
end TestContainersSpec

object MdocSession:
  object App:
    case class GpsException()
        extends RuntimeException
