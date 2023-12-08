package resourcemanagement

import zio.*
import zio.direct.*

import java.io.{ByteArrayInputStream, InputStream}

object Resourcefulness extends ZIOAppDefault:

  def foo(inputStream: InputStream) =
    println("closing the inputstream")
    inputStream.close()

  def bar(inputStream: InputStream) =
    foo(inputStream)
    inputStream.read()

  def doIt() =
    val inputStream = ByteArrayInputStream("hello, world".getBytes)
    bar(inputStream)

  val helloWorld = ZIO.fromAutoCloseable:
    ZIO.succeed(ByteArrayInputStream("hello, world".getBytes)).debug

  val uppercaseInputStream =
    defer:
      val inputStream = helloWorld.run
      val s = ZIO.attempt(String(inputStream.readAllBytes())).run
      s.toUpperCase

  val countString =
    defer:
      val inputStream = helloWorld.run


  //def zDoSomethingWithResource() =


  def run =
    doIt()
    ZIO.unit

  