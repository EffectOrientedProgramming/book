package prep

import java.io.IOException

import zio.ZIO

def putStrLn(s: String): ZIO[Any, IOException, Unit] =
  ZIO.fail(new IOException("asdf"))
