Managing and wiring dependencies has been a perennial challenge in software development.

Imagine a `ServiceX` that is needed by 20 diverse functions across your stack.
Usually `ServiceX` has exactly one instance/implementation should be used throughout your application.

```scala mdoc
import zio.ZIO

trait ServiceX:
    val retrieveImportantData: ZIO[Any, Nothing, String]

object ServiceImplementation extends ServiceX:
    val retrieveImportantData: ZIO[Any, Nothing, String] = ???

```
{{ TODO: Should we show a class-based approach, or just go straight to functions? }}
```scala mdoc
class UserManagement(serviceX: ServiceX)

class StatisticsCalculator(serviceX: ServiceX)

class SecurityModule(serviceX: ServiceX)

class LandingPage(statisticsCalculator: StatisticsCalculator)
```

```scala mdoc
class Application(userManagment: UserManagement, securityModule: SecurityModule, landingPage: LandingPage)

def construct(): Application =
    Application(
        UserManagement(ServiceImplementation),
        SecurityModule(ServiceImplementation),
        LandingPage(
            StatisticsCalculator(
                ServiceImplementation
            )
        )
    )

```

Even in this tiny example, the downsides are already starting to show.

- We have to copy/paste `ServiceImplementation` numerous times
- We have to manage multiple levels of dependencies. `LandingPage` and `ServiceImplentation` have to be manually connected.