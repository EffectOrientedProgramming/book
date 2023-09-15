## How to Wow
- Successful, moderately complex application startup
- Compilation errors whenever something is missing or conflicting in the dependency graph
- Demonstrate runtime errors during application startup
- Swap out implementations for testing

With ZIO's approach to dependencies, you get all the desirable characteristics at compile-time, using standard language features.
Your services are defined as classes with constructor arguments, just as in any vanilla Scala application.
No annotations that kick off impenetrable wiring logic outside your normal code.
For any given service in your application, you define what it needs in order to execute.
Finally, when it is time to build your application, all of these pieces can be provided in one, flat space.
Each component will automatically find its dependencies, and make itself available to other components that need it.

To aid further in understanding your application architecture, you can visualize the dependency graph with a single line.

You can also do things that simply are not possible in other approaches, such as sharing a single instance of a dependency across multiple test classes, or even multiple applications.

## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/04_Dependency_Injection.md)


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/dependency_injection/Wow.scala
```scala
package dependency_injection

case class A()
case class B()
case class C()
case class D(b: B, c: C)
val d: ZLayer[B & C & Scope, Nothing, D] =
  ZLayer.fromZIO:
    defer:
      val dLocal =
        D(ZIO.service[B].run, ZIO.service[C].run)
      acquireReleaseDebugOnly(dLocal).run

def acquireReleaseDebugOnly[T: Tag](
    instance: T
) =
  val rep =
    instance
      .getClass
      .toString
      .dropWhile(_ != '.')
      .drop(1)
      .replace("$", "")
  ZIO.acquireRelease(
    defer:
      val duration =
        Random.nextIntBounded(500).run
      ZIO.sleep(duration.millis).run
      ZIO.debug(s"Getting $rep").run
      instance
  )(_ =>
    defer:
      val duration =
        Random.nextIntBounded(500).run
      ZIO.sleep(duration.millis).run
      ZIO.debug(s"Releasing $rep").run
  )
end acquireReleaseDebugOnly

def acquireReleaseDebug[T: Tag](instance: T) =
  ZLayer
    .fromZIO(acquireReleaseDebugOnly(instance))

object Wow extends ZIOAppDefault:
  def run =
    defer:
      ZIO.service[A].run
      ZIO.service[B].run
      ZIO.service[C].run
      ZIO.service[D].run
    .provide(
      Scope.default,
      acquireReleaseDebug(A()),
      acquireReleaseDebug(B()),
      acquireReleaseDebug(C()),
      d
    )

```

