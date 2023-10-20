# Layers

1. Creating
1. Composing

Managing and wiring dependencies has been a perennial challenge in software development.

ZIO provides the `ZLayer` class to solve many of the problems in this space.
If you pay the modest, consistent cost of constructing pieces of your application as `ZLayer`s, you will get benefits that scale with the complexity of your project.
Consistent with `ZIO` itself, `ZLayer` has 3 type parameters that represent:

- What it needs from the environment
- How it can fail
- What it produces when successful.

With the same type parameters, and many of the same methods, you might be wondering why we even need a separate data type - why not just use `ZIO` itself for our dependencies?
The environment type parameter for `ZLayer` maps directly to unique, singleton services in your application.
The environment type parameter for `ZIO` might have *many* possible instances.
`ZLayer` provides additional behaviors that are valuable specifically in this domain.
Typically, you only want a single instance of a dependency to exist across your application.
This may be to reduce memory/resource usage, or even to ensure basic correctness.
`ZLayer` output values are shared maximally by default.
They also build in scope management that will ensure resource cleanup in asynchronous, fallible situations.


==============

Imagine a `ServiceX` that is needed by 20 diverse functions across your stack.
Usually `ServiceX` has exactly one instance/implementation should be used throughout your application.

```scala mdoc
case class ServiceX():
  val retrieveImportantData
      : ZIO[Any, Nothing, String] = ???
```
{{ TODO: Should we show a class-based approach, or just go straight to functions? }}
```scala mdoc
case class UserManagement(serviceX: ServiceX)

case class StatisticsCalculator(
    serviceX: ServiceX
)

case class SecurityModule(serviceX: ServiceX)

case class LandingPage(
    statisticsCalculator: StatisticsCalculator
)
```

## Historic Approaches

### Manual Wiring

```scala mdoc
case class Application(
    userManagment: UserManagement,
    securityModule: SecurityModule,
    landingPage: LandingPage
)

def construct(): Application =
  val serviceX = ServiceX()
  Application(
    UserManagement(serviceX),
    SecurityModule(serviceX),
    LandingPage(StatisticsCalculator(serviceX))
  )
```

Even in this tiny example, the downsides are already starting to show.

- We have to copy/paste `serviceX` numerous times
- We have to manage multiple levels of dependencies. `LandingPage` and `ServiceImplentation` have to be manually connected.


### Annotations

Pros
- "Easy" in the sense that they do not require much code at the use-site
- Smoother refactoring, as the injection system will determine what needs to be passed around

Cons
- Does not follow normal control flow or composition
- Typically, relies on some framework-level processing that is not easily controlled by the user
