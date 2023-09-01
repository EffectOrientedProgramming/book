# Concurrency - Interruption

```scala mdoc
runDemoValue(
  ZIO.raceAll(
    sleepThenPrint(2.seconds),
    Seq(sleepThenPrint(1.seconds))
  )
)
```