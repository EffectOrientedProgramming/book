## resourcemanagement

 

### experiments/src/main/scala/resourcemanagement/ChatSlots.scala
```scala
package resourcemanagement

import zio.Console.printLine

case class Slot(id: String)
case class Player(name: String, slot: Slot)

object ChatSlots extends zio.ZIOAppDefault:
  enum SlotState:
    case Closed,
      Open

  def run =

    @annotation.nowarn
    def acquire(ref: Ref[SlotState]) =
      defer:
        printLine:
          "Took a speaker slot"
        .run
        ref
          .set:
            SlotState.Open
          .run
        "Use Me"

    def release(ref: Ref[SlotState]) =
      defer:
        printLine:
          "Freed up a speaker slot"
        .orDie
          .run
        ref
          .set:
            SlotState.Closed
          .run

    defer {
      val ref =
        Ref
          .make[SlotState]:
            SlotState.Closed
          .run
      val managed =
        ZIO.acquireRelease(acquire(ref))(_ =>
          release:
            ref
        )
      val reusable =
        managed.map:
          printLine(_)
      reusable.run
      reusable.run
      ZIO
        .scoped:
          // TODO Get rid of flatmap if
          // possible...
          managed.flatMap: s =>
            defer:
              printLine:
                s
              .run
              printLine:
                "Blowing up"
              .run
              if (true)
                throw Exception:
                  "Arggggg"
        .run
    }
  end run
end ChatSlots

```


### experiments/src/main/scala/resourcemanagement/Resourcefulness.scala
```scala
package resourcemanagement

import zio.*
import zio.direct.*

import java.io.{
  ByteArrayInputStream,
  InputStream
}

object Resourcefulness extends ZIOAppDefault:

  def foo(inputStream: InputStream) =
    println("closing the inputstream")
    inputStream.close()

  def bar(inputStream: InputStream) =
    foo(inputStream)
    inputStream.read()

  def doIt() =
    val inputStream =
      ByteArrayInputStream(
        "hello, world".getBytes
      )
    bar(inputStream)

  val helloWorld =
    ZIO.fromAutoCloseable:
      ZIO
        .succeed(
          ByteArrayInputStream(
            "hello, world".getBytes
          )
        )
        .debug

  val uppercaseInputStream =
    defer:
      val inputStream = helloWorld.run
      val s =
        ZIO
          .attempt(
            String(inputStream.readAllBytes())
          )
          .run
      s.toUpperCase

  val countString =
    defer:
      val inputStream = helloWorld.run

  // def zDoSomethingWithResource() =

  def run =
    doIt()
    ZIO.unit
end Resourcefulness

```


