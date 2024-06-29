# Effect Superpowers


Once programs are defined in terms of Effects, we use operations from the Effect System to add new functionality.
Combining Effects with these operations feels like a superpower.
The reason we call them "superpowers" is that the operations can be attached to **any** Effect.
Operations can even be chained together.

Common operations like `timeout` are applicable to all Effects while some operations like `retry` are only applicable to a subset of Effects.

Ultimately this means we do not need to create bespoke operations for the different Effects in our system.

To illustrate this we will show a few examples of common operations applied to Effects.
Let's start with the "happy path" where we save a user to a database (this is an Effect) and then gradually add superpowers.

Here we save `userName` to a database via `saveUser`:

```scala
val userName =
  "Morty"
```

```scala
val effect0 =
  saveUser:
    userName
```

Instead of parentheses to delimit function arguments, we use Scala’s newer “colon plus indent” (*significant indentation*) syntax.
Here, `saveUser` is the function and `userName` is the single argument to that function.

`effect0` is a value containing the code that produces the Effect.
Note that defining `effect0` does not execute that code, it only holds it so it can be run at some later time.
This is an example of *deferred execution*, described in the Introduction.
By deferring the execution of an Effect, we can add functionality to that Effect.

Effects can be:
- Run as "main" programs
- Embedded in other programs
- Embedded in tests.

To run an Effect as a "main" program, we use the ZIO library which contains an object called `ZIOAppDefault`:

```scala
object MyApp extends ZIOAppDefault:
  def run =
    effect0
```

The overridden value of `run` must be an Effect.
`run` is special and passes the Effect to `MyApp`, which runs it.

For noise reduction we’ve been able to shorten this to:

```scala
def run =
  effect0
```

Output:

```shell
Result: User saved
```

By default, `effect0` runs in the "happy path" so it will not fail.

We can specify the way an Effect runs. 
To do this, we configure the program by overriding the `bootstrap` value. 
A `bootstrap` creates a scenario for the execution of the program.
Here we explicitly provide the happy path:

```scala
override val bootstrap =
  happyPath

def run =
  effect0
```

Output:

```shell
Result: User saved
```

Don’t assume the happy path or you’ll end up with strange unhandled errors lurking in your code.

We can override the `bootstrap` value to simulate failure:

```scala
override val bootstrap =
  neverWorks

def run =
  effect0
```

Output:

```shell
Log: **Database crashed!!**
Result: **Database crashed!!**
```

This program logs and returns the failure.

## Retry

Sometimes things work when you keep trying.  
We can retry `effect0` by attaching the `retryN` operation:

```scala
val effect1 =
  effect0.retryN(2)
```

`effect0.retryN` becomes a new Effect and is assigned to a `val` to create `effect1`, which also has delayed execution.
Almost always, when you apply an operation to an Effect, you get a new Effect.

We run the new Effect in the scenario `doesNotWorkInitially` which works on the third try:

```scala
override val bootstrap =
  doesNotWorkInitially

def run =
  effect1
```

Output:

```shell
Log: **Database crashed!!**
Log: **Database crashed!!**
Result: User saved
```

From the output you can see that running the Effect works after the initial attempt plus two retries.

### What If It Never Succeeds?

In the `neverWorks` scenario, the Effect fails its initial attempt and all subsequent retries:

```scala
override val bootstrap =
  neverWorks

def run =
  effect1
```

Output:

```shell
Log: **Database crashed!!**
Log: **Database crashed!!**
Log: **Database crashed!!**
Result: **Database crashed!!**
```

After the failed retries, the program returns an error.

## Modify Error

Let's attach a nicer error onto the previously defined operations (the retries). 
We use `orElseFail` to transform the failure into a user-friendly error:

```scala
val effect2 =
  effect1.orElseFail:
    "ERROR: User could not be saved"
```

`orElseFail` is attached to the prior Effect that contains the retry.
This creates a new Effect that has both error handling operations.

Running this new Effect in the `neverWorks` scenario produces the error:

```scala
override val bootstrap =
  neverWorks

def run =
  effect2
```

Output:

```shell
Log: **Database crashed!!**
Log: **Database crashed!!**
Log: **Database crashed!!**
Result: ERROR: User could not be saved
```

We alter the behavior but without restructuring the original Effect.
## Timeout

Sometimes an Effect fails quickly, as we saw with retries.
Sometimes an Effect that takes too long is itself a failure.
The `timeoutFail` operation can be chained to our previous Effect to specify a maximum time the Effect can run before producing an error:

```scala
val effect3 =
  effect2
    .timeoutFail("** Save timed out **"):
      5.seconds
```

`timeoutFail` takes a single String argument which we parenthesize.
The result of this call is a function that also takes a single argument that we pass using significant indentation.
Although we prefer significant indentation whenever possible, sometimes the code is easier to read by introducing parentheses. 

If the Effect does not complete within the time limit, it is canceled and returns our error message.
Timeouts can be added to any Effect.

The `slow` scenario runs longer than our specified time limit of five seconds:

```scala
override val bootstrap =
  slow

def run =
  effect3
```

Output:

```shell
Result: ** Save timed out **
```

The Effect takes too long and produces the error.

## Fallback

A failing Effect can fall back to a different strategy.
One option is to use `orElse` with a fallback operation to run when an Effect fails:

```scala
val effect4 =
  effect3.orElse:
    sendToManualQueue:
      userName
```

`sendToManualQueue` happens when the user can't be saved.

Let's run the new Effect in the `neverWorks` scenario to ensure we reach the fallback:

```scala
override val bootstrap =
  neverWorks

def run =
  effect4
```

Output:

```shell
Log: **Database crashed!!**
Log: **Database crashed!!**
Log: **Database crashed!!**
Result: Please manually provision Morty
```

The retries do not succeed so the fallback is applied.

## Logging

To ensure that some logging happens after the logic completes, regardless of failures, we use `withFinalizer`:

```scala
val effect5 =
  effect4.withFinalizer:
    _ => logUserSignup
```

`withFinalizer` expects a function as its argument; `_ => logUserSignup` creates a function that takes no arguments and calls `logUserSignup`.
`withFinalizer` attaches this behavior without changing the types contained in the original Effect.

```scala
override val bootstrap =
  happyPath

def run =
  effect5
```

Output:

```shell
Result: User saved
```

We can add numerous behaviors to an Effect regardless of that Effect’s error and result types.

## Timing

For diagnostic information you can track timing:

```scala
val effect6 =
  effect5.timed
```

```scala
override val bootstrap =
  happyPath

def run =
  effect6
```

Output:

```shell
Result: (PT0.016316058S,User saved)
```

We run the Effect in the "HappyPath" Scenario; now the timing information is packaged with the original output `String`.

## Filtering

Our lead engineer tells us a certain user should be prevented from using our system.
We use `when` to exclude Morty:

```scala
val effect7 =
  effect6.when(userName != "Morty")
```

```scala
override val bootstrap =
  happyPath

def run =
  effect7
```

Output:

```shell
Result: None
```

We added behavior that prevents an Effect from executing—to the *end* of a complex Effect. Consider the work necessary to do this without an Effect System.

## Effects Are The Sum of Their Parts

These examples show only a glimpse of the superpowers we can add to *any* Effect.
There are many other behaviors we can attach to any Effect.

We started with:
- `effect0`: Save User

Effects 1 - 7 are new Effects, each built on the previous Effect:
- `effect1`: Retry
- `effect2`: Modify Error
- `effect3`: Timeout
- `effect4`: Fallback
- `effect5`: Logging
- `effect6`: Timing
- `effect7`: Filtering

Each `effect*` is independent.
You can mix and match the retries, fallbacks, etc., however you want.
You can easily create new Effects that have new superpowers.

## Deferred Execution

If Effects ran immediately, we could not freely add behaviors:
- We cannot timeout something that might have started running, or has already completed.
- We cannot retry something if we only hold the completed result.
- We cannot parallelize operations if they have already started single-threaded execution.

When we manage an Effect, we hold a value that represents something that _can_ be run, but hasn't yet.
With that, the Effect System can freely add behavior before/after that value.

Because Effects are deferred and independent, we can combine them in a variety of ways.

The most common way to combine Effects is sequentially.
You might think this should work:

```scala
def run =
  Console.printLine("Before save")
  effect1
```

Output:

```shell
Result: User saved
```

The result returned by `run` is the final value of the function: `effect1`.
The Effect System takes `effect1` returned by `run` and only runs that.
Since Effects are deferred, `Console.printLine` never runs.

To sequence multiple Effects, we construct a new `Effect` that contains the sequence.
`defer` produces a new Effect containing a sequence of other Effects:

```scala
def run =
  defer:
    Console.printLine("Before save").run
    effect1.run // Display each save
```

Output:

```shell
Before save
Result: User saved
```

A `defer` block creates a new Effect, which is returned by `run`.
The `.run` called on each Effect constructs the sequence.
Even though we say `.run`, the Effects are still deferred.
They get run, in order, when the Effect produced by the `defer` block is run.

The `.run` method is only available for Effect values.
We explicitly call `.run` whenever we want to sequence Effects.
If we do not call `.run`, we end up with an un-executed Effect.
We want this explicit control so we can attach operations to our Effects before we run them.

We can assign the new Effect to a `val` like we did with `effect1` - `effect7`:

```scala
val effect8 =
  defer:
    Console.printLine("Before save").run
    effect1.run
```

When you finish assembling your Effect and are ready to run it, you utilize the other important `run` method:

```scala
val run =
  effect8
```

Output:

```shell
Before save
Result: User saved
```

Having 2 versions of `run` seems confusing, but they serve different purposes:
- Attaching `.run` to Effects in a `defer` establishes the order of execution for that Effect.
  This can happen many times throughout your program.
- Assigning an Effect to `def run` actually executes the program.
  This typically happens only once in your code.

### The `.run` Method

Calling `.run` on anything other than an Effect produces an error:


```scala
val program =
  defer:
    (1 + 1).run
```

Output:

```shell
error:
value run is not a member of Int.
An extension method was tried, but could not be fully constructed:

    run[R, E, A](1.+(1))

    failed with:

        Found:    (2 : Int)
        Required: ZIO[Nothing, Any, Any]
      .repeatN(1).run
              ^
```

We can also call `.run` after chaining operations onto an Effect:

```scala
def run =
  defer:
    effect8
      .debug // Display each save
      .repeatN(1).run
```

Output:

```shell
Before save
User saved
Before save
User saved
Result: User saved
```

The chain produces a new Effect.
Calling `.run` executes that new Effect.
Since `.debug` appears before `repeatN(1)`, `effect8.debug` executes once and is then repeated once.

We _cannot_ repeat our executed Effect by putting `.run` in the middle of the chain:

```scala
val programManipulatingBeforeRun =
  defer:
    effect8.run.repeatN(3)
```

Output:

```shell
error:
value repeatN is not a member of String - did you mean String.repeat?
      .repeatN(1).run
      ^
```

Running an Effect produces its result, not the deferred computation. Thus there’s no appropriate place to attach `repeatN(3)`.

All calls to `.run` must happen within a `defer` block, so when `effect8` is defined, we haven’t executed anything—we’ve only created a new Effect.
A `defer` block creates a new Effect that describes a program that knows the order in which to execute the individual Effects.
But that Effect only runs when the program is executed.

```scala
val surroundedProgram =
  defer:
    Console.printLine("**Before**").run
    effect8
      .debug // Display each save
      .repeatN(1).run
    Console.printLine("**After**").run
```

`surroundedProgram` only runs when we pass it to the Effect System:

```scala
def run =
  surroundedProgram
```

Output:

```shell
**Before**
Before save
User saved
Before save
User saved
**After**
```

Deferred execution can seem strange at first, but it is essential for inserting new functionality into the Effects in our program.