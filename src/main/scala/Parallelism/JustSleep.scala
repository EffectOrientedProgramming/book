package Parallelism

import java.io.IOException
import zio.console.{Console, getStrLn, putStrLn}
import zio.duration.*
import zio.{App, IO, Runtime, ZIO, ZLayer, UIO, Fiber}

import scala.concurrent.Await

object JustSleep extends App:

  override def run(args: List[String]) =
    ZIO.collectAllPar((1 to 10000).map(_ => ZIO.sleep(1.seconds))).exitCode

@main def ToFuture() =
  Await.result(Runtime.default.unsafeRunToFuture(ZIO.sleep(1.seconds)), scala.concurrent.duration.Duration.Inf)