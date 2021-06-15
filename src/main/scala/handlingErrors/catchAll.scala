package handlingErrors

import zio.*
import zio.console.*
import java.io.IOException
import java.lang.ArithmeticException


object catchAll extends zio.App :


  def logic:ZIO[Any,
    Throwable, Unit] =
      throw new IOException("Boom")
      throw new ArithmeticException()
      throw new Exception()

  def standin:ZIO[console.Console, IOException, Unit] =
    putStrLn("Im a standin")

  def run(args: List[String]) =
    /* logic.catchAllDefect(x =>
      putStrLn("Ultimate Error Message: " + x.getMessage)
  )
      .exitCode
     */
    standin.exitCode


