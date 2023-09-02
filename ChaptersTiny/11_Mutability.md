# Mutability

```scala mdoc
// This is lazy *purely* to silence the mdoc output.
// TODO Decide whether it's clearer to do this, or capture everything in an object
lazy val unreliableCounting =
  var counter = 0
  val increment =
    ZIO.succeed {
      counter = counter + 1
    }

  defer {
    ZIO
      .foreachParDiscard(Range(0, 100000))(_ =>
        increment
      )
      .run
    "Final count: " + ZIO.succeed(counter).run
  }

runDemo(unreliableCounting)
```

## Reliable Counting

```scala mdoc
lazy val reliableCounting =
  def incrementCounter(counter: Ref[Int]) =
    counter.update(_ + 1)

  defer {
    val counter = Ref.make(0).run
    ZIO
      .foreachParDiscard(Range(0, 100000))(_ =>
        incrementCounter(counter)
      )
      .run
    "Final count: " + counter.get.run
  }

runDemo(reliableCounting)
```

```scala mdoc
def expensiveCalculation() = Thread.sleep(35)
```

```scala mdoc
def sendNotification() =
  println("Alert: We have updated our count!!")
```

```scala mdoc
lazy val sideEffectingUpdates =
  defer {
    val counter = Ref.make(0).run
    ZIO
      .foreachParDiscard(Range(0, 4))(_ =>
        counter.update { previousValue =>
          expensiveCalculation()
          sendNotification()
          previousValue + 1
        }
      )
      .run
    "Final count: " + counter.get.run
  }

// Mdoc/this function is showing the notifications, but not the final result
runDemo(sideEffectingUpdates)
```

## Ref.Synchronized

```scala mdoc
lazy val sideEffectingUpdatesSync =
  defer {
    val counter = Ref.Synchronized.make(0).run
    ZIO
      .foreachParDiscard(Range(0, 4))(_ =>
        counter.update { previousValue =>
          expensiveCalculation()
          sendNotification()
          previousValue + 1
        }
      )
      .run
    "Final count: " + counter.get.run
  }

runDemo(sideEffectingUpdatesSync)
```