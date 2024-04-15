# Hello, Output


```scala mdoc:invisible
enum Scenario:
  case HappyPath
  case NeverWorks

val scenarioConfig: Config[Option[Scenario]] =
  Config.Optional[Scenario](Config.fail("no default scenario"))

class StaticConfigProvider(scenario: Scenario) extends ConfigProvider:
  override def load[A](config: Config[A])(implicit trace: Trace): IO[Config.Error, A] =
    ZIO.succeed(Some(scenario).asInstanceOf[A])

val happyPath =
  Runtime.setConfigProvider(StaticConfigProvider(Scenario.HappyPath))

val neverWorks =
  Runtime.setConfigProvider(StaticConfigProvider(Scenario.NeverWorks))
  
def saveUser(username: String) =
  defer:
    val maybeScenario = ZIO.config(scenarioConfig).run
    Console.printLine(maybeScenario).run
    Console.printLine(s"saved $username").run
```

```scala mdoc:silent
val effect0 =
  saveUser("bob")
```

```scala
object MyApp extends ZIOAppDefault:
  def run =
    effect0
```

```scala mdoc:runzio
def run =
  effect0
```

```scala mdoc:runzio
override val bootstrap =
  happyPath

def run =
  effect0
```

```scala mdoc:runzio
override val bootstrap =
  neverWorks

def run =
  effect0
```
