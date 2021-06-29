package directoryExample

import zio.{UIO, ZIO}
import exIOError.errorAtNPerc
import java.io.IOException

def finalizer(source: scala.io.Source) = //Define the finalizer behavior here
  UIO.effectTotal {
    println("Finalizing: Closing file reader")
    source.close //Close the input source
  }

val readFileContents: ZIO[Any, Throwable | IOException, Vector[String]] =
  ZIO(
    scala.io.Source.fromFile("src/main/scala/directoryExample/firmData.csv")
  ) //Open the file to read its contents
    .bracket(finalizer) {
      bufferedSource => //Use the bracket method with the finalizer defined above to define behavior on fail.

        val lines =
          for line <- bufferedSource.getLines
          yield line
        //This is where you can set the error likely hood
        //This models a fatal IOException
        errorAtNPerc(10) //ie, 10 % chance to fail...
        ZIO.succeed(Vector() ++ lines)
    }
