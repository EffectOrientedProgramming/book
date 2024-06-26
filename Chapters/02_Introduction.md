# Introduction

Most existing languages are built for rapid development.
You build a system as quickly as possible, then begin isolating areas of failure, finding and fixing bugs until the system is tolerable and can be delivered.

Over the lifetime of a system, new needs are discovered and the system is adapted to meet those needs.
Many of these adaptations don’t conform to the original vision and architecture of the system, and will be forced in.
Each forced feature degrades the structure and integrity of the system, and makes additional features even harder to force in.
This degradation is commonly called *technical debt.*
It’s debt because you are accumulating costs that must be borne by future programmers.
The idea is that you will one day stop adding new features, and pay down the accumulated debt by rewriting the system without adding new features (this is called *refactoring*).

Often the debt never gets paid down.
The system eventually becomes un-maintainable.
It is difficult or even impossible to add new functionality.

Bruce’s father was a builder and, when people wanted to remodel their house, they would often desire enough changes that it was cheaper and more sensible to tear the house down and start over.
This point was reached far sooner than the owners imagined.

For the past generation of languages, it made sense to focus on rapid development.
That was the most pressing problem.
Although rapid development will always be important, we have reached a new era where *modification of existing systems* is paramount.

It is expensive and impractical to rewrite a system that is overwhelmed with debt.
The costs are numerous, especially because the business probably can’t run without the software:
- You need programmers to maintain the existing software while the new system is developed. 
  This means continuing to force in new features as it gets harder and harder.
  At the end, the software you’ve been working on is discarded and you might become redundant.
  None of this makes for a desirable job. 
- You need an additional team to create the new system.
- You have no certainty that the new project will succeed, or when.
- New functionality must be incorporated into both the old and new systems.
- You have all the problems of software development multiplied by (at least) two.

We need to change systems rather than rewrite them.
We will always want rapid development, but we also need easy adaptability.
What if we could shift our thinking around the problem of building software systems?
When building a system from pieces, the parts will not become buried within the whole.
They will still be accessible, and changeable.
The entire system becomes far more adaptable.

It’s hard to imagine what this means, and it does require a new way of thinking about programming.
The goal of this book is to introduce you to that new way of thinking.
## The Pursuit of Adaptability

Imagine you want to create a system to build homes by assembling room modules.
Each type of room has doors and windows, and there's a way to plug them together.
By selecting pieces with compatible doors and windows, you can assemble a house.

This concept of *composability* has arguably been the prime objective of programming since we raised ourselves from the swamps of assembly language.
We want to take smaller pieces and easily compose them into larger pieces, which can themselves be composed.
Over the decades, the programming community has made great strides in this endeavor.
Each time we figure something out and make a leap forward, however, we inevitably run into the next wall.

Our housing example is a decent reflection of where most programming is now: we have chunks of code—modules—and we can put them together.
We have improved our type systems and the ways we create data structures.
What wall are we facing now?

Returning to our home-building system, we've solved the problem of assembling rooms, but adding functionality to those rooms is quite difficult.
If we want electricity in a room, we have to tear up the walls and insert electrical conduits.
To add a vent we must tunnel up through the building to the roof.
Adding plumbing is very challenging because it runs through the concrete foundation and the walls.
We can assemble rooms, but if we want a room to do anything interesting, we must remodel the house.

Consider a component that gets information from a server, processes it and then displays it.
What happens if you put this component into use and then discover that the server is flaky?
Perhaps it occasionally drops requests, or takes too long.
There are different strategies for this: retrying, backoff, querying other servers, etc.
The problem is that, like the home-building system, you must go into your module and rebuild it.
This takes time and effort and complicates the code.
We would prefer to just attach our new functionality to the existing code without rewriting it.

In the home-building system, what if each room contains a channel, and when you assemble rooms, the channels match up?
Now if you want plumbing, electricity, venting, network cabling, etc., you just run it through the channel.
New features can be added to rooms without rebuilding the house.

This book introduces *Effect Systems*, which allow you to do the same thing for software as we have done for home-building: Add features without rewriting the software.

## What's Stopping Us?

We don't have the home-building system’s channel.
It's hard to imagine what that channel would look like, or how it behaves.
To get there we must examine some basic issues.
A dominant issue is *predictability*.

Consider a simple function:

```scala 3 mdoc:compile-only
def fp(a: Int, b: Int): Int =
  a + b
```

`fp` is completely predictable:
- `fp(a, b)` always produces an `Int` result.
- It never fails.
- The same inputs always produce the same outputs.
- It’s so consistent that instead of calling the function you could just look up results in a table (it can be *cached*).

A predictable function has a special name: *pure*.

If we include something unpredictable in a pure function, the results become unpredictable.
Here, we add a random number:

```scala 3 mdoc:compile-only
val rand =
  new scala.util.Random

def fu(a: Int, b: Int): Int =
  a + b + rand.nextInt()
```

Not surprisingly, adding a random number to the result takes us from predictable to unpredictable.
`fu` never fails and always produces an `Int`, but if you call it twice with the same inputs, you get different outputs.

Unpredictable elements are called *Effects*.

## Managing Effects

What if we could control an Effect by putting it in a kind of box?
Instead of using `scala.util.Random`, we can make our own random number generator:

```scala 3 mdoc:invisible manuscript-only
class ControlledRandom
    extends scala.util.Random
```

```scala 3 mdoc:compile-only
val rand =
  new ControlledRandom

def fc(a: Int, b: Int): Int =
  a + b + rand.nextInt()
```

`ControlledRandom` presumably uses `scala.util.Random`, but it could contain anything else.
For example, we could swap in a custom generator to produce controlled results for testing `fc`.

Through `ControlledRandom`, we control the output of `rand`.
If we provide a certain set of inputs (including one for `rand`), we can predict the output.
Once again, the function is pure.

We achieve this by *managing* the Effect.
However, managing an Effect means we not only control *what* results are produced by `rand`, we also control *when* those results are produced.
The control of *when* is called *deferred execution*.
Deferred execution is part of the solution for easily attaching functionality to an existing program.

Consider the module that gets data from a server, processes it, then displays it.
That code is executed all at once.
If the server we’re trying to connect to is flaky and we’d like to add a retry, we don’t have direct access to the server call, which is hidden behind a wall of code.

Now let’s treat the call to the server as an Effect.
We manage it by putting a box around the server Effect like we did with `ControlledRandom`.
Because the execution of that Effect is now deferred, we have the option of attaching the retry (or another strategy such as a timeout) directly to that Effect, when it is executed.
Deferred execution adds a “cut point” where we can insert functionality on any Effect. 
Effects are the unpredictable points in a program, and thus comprise most of the places where we are likely to want to insert such functionality.

This still sounds complicated.
It’s hard to imagine how to write this kind of code.
Fortunately, *Effect Systems* provide the structure for you.
An Effect System enables us to add almost any functionality to a program.

Now it sounds *too* simple—just add an Effect System!
This still requires a significant shift in the way you think about programming.
Also, an Effect System includes libraries, some of which you use instead of the libraries you know.
It will take time and effort to rewire your brain into this new mode of thinking.
The goal of this book is to give you a gentle start along this path.

## Types of Effects

An Effect is an interaction, usually with an external system, that is inherently unpredictable.
For example, a function that displays the current date must ask some other system for that information—usually just the operating system, which keeps track using a clock chip that lives outside the main processor (though usually on the same physical chip).
Such functions are unpredictable because the programmer has no control over what that external system will say or do.

When you run an Effect, you often change the world:
- Printing a 3D figurine means you cannot reclaim that material.
- A Tweet can be deleted after sending, but people might have already read it.
- A database `DELETE` statement can be paired an `INSERT` statement that leaves the database in the same state that it started in.
  However, this must still be considered Effectful.
  Another program might read your data before you delete it, or a database trigger might activate during an `INSERT`.
- Saying, "You are getting a raise" to someone creates an Effect that may not be reversible.

Once a program runs an Effect, the impact is out of our control and cannot be undone.

There are many types of Effects:
- Accept user input
- Read from a file
- Get the current time from the system clock
- Generate a random number
- Display to a screen
- Write to a file
- Mutate a variable
- Save to a database
- And more...

These can have domain-specific forms:
- Get the current price of a stock
- Detect the electrical current from a pacemaker
- Check the temperature of a nuclear reactor
- 3D print a model
- Trigger an alarm
- Sense slippage in an anti-lock braking system
- Stabilize an airplane
- Detonate explosives

### Side Effects

You’ve probably heard about *Side Effects.*
A Side Effect happens when you call a function and something changes in your program or in the environment.
So you don’t just get a result from your function call, you change your surroundings—and this might also affect your function the next time you call it, or even the behavior of the rest of the program.

We hope that at this point you are thinking, “Isn’t that what we’ve been talking about? Isn’t that just an Effect?”

The difference between a Side Effect and an Effect is that a side effect “just happens,” while an Effect is something that can be managed. 
For example, suppose you write to the console using the standard console library provided by your language.
That library was not designed for Effect Systems, so we don’t have any way to put a box around it and control it like we do with Effects.
When you write to the console, the surroundings change (output appears on the console) and there’s nothing we can do about it.
That’s a Side Effect.

For this reason, we must often use special libraries from the Effect System when performing some kinds of Effects such as console I/O.
Those libraries are written to keep track of and manage the Effects.

The simple answer is that Side Effects are un-managed and Effects are managed.

### `Unit` and Effects

`Unit` can be viewed as the bare minimum of Effect tracking.

Consider `saveInformation`, which has a type of `String => Unit`.

```scala 3 mdoc manuscript-only
def saveInformation(info: String): Unit =
  ???
```

Returning `Unit` is the simplest indication of an Effectful function.
You don't call it to produce a result value. 
We know there must be a Side Effect—there's no other reason to call it.

If a function has no parameters, this is equivalent to a `Unit` argument.
In this case, an Effect is used inside the function to produce the result.

A function that has a `Unit` argument and `Unit` return only produces Side Effects.
### Failures

Failures, especially the way we currently report and handle them using exceptions, are another form of unpredictability.

Not only are failures themselves unpredictable, exceptions are not part of the type system.
This means that when you call a function, you cannot reliably know what exceptions might emerge from that function call (some languages have attempted a *parallel type system* via *exception specifications* but these experiments have universally failed).
By moving failure information into the type system, the type checker ensures that all possible failures are accounted for in your code.

Because failure is unpredictable, it is another kind of Effect.
An Effect System also manages failure. 

A function typically returns the anticipated “answer,” even though you know that might not happen—there could be failures. 
An Effect System creates a structure as the return type from a function.
This structure contains the expected “answer,” but it also contains failure information.
Every possible failure type is enumerated in this return structure.
Like everything else, the return structure has a type—and this incorporates all the failure types.
Failures are now part of the type system.
The type-checking system can now determine, from this return type, whether your code handles all possible failures.

The return type also includes the other (non-failure) Effect types.
This new return type is what provides the channel that we need to enable easily-adaptable systems.
## Improving Your Life

Effect Systems make it easy to add functionality to existing code.
For example, you can add a time-out to any Effect to control its maximum duration.
Applying such operations feels like a superpower, and that's what we show in the next chapter.

Learning about Effect Systems requires patience.
With most languages, the first thing you learn is to write "Hello, World!"
You then accumulate additional language features as standalone concepts.
Effect Systems require a shift in perception of what code is, and how it executes.
We hope to introduce Effect Systems in a way that is not overwhelming, so you are inspired to keep working toward that shift.
