
```scala mdoc
runDemo(
  Console.printLine("hi!!!")
)
```

```scala mdoc
runDemo(
  Console.printLine("hello!")
)
```


```scala mdoc
runDemo(
  ZIO.succeed(scala.Console.println("Failure!!!"))
)
```
          

```scala mdoc
runDemo(
  ZIO.succeed(println("hi"))
)
```
