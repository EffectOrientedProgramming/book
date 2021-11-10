package zioBasics

import zio.*

val i1: Int = 1
val i2      = 1

val u1: UIO[Unit]               = ZIO.unit
val u2: ZIO[Any, Nothing, Unit] = u1

val a1: UIO[Int]               = ZIO.succeed(1)
val a2: ZIO[Any, Nothing, Int] = a1

val e1: IO[String, Nothing] = ZIO.fail("Error")
val e2: ZIO[Any, String, Nothing] = e1

val m: UIO[String] =
  a1.map { i =>
    s"i = $i"
  }

val f: UIO[String] =
  a1.flatMap { i =>
    ZIO.succeed(s"i = $i")
  }
