# Introduction

Imagine you want to create a system to build homes by assembling room modules.
Each type of room has doors and windows, and there's a way to plug them together.
By selecting pieces with compatible doors, and windows where you want them, you can assemble a house.

This concept of *composability* has arguably been the prime objective of programming since we raised ourselves from the swamps of assembly language.
We want to be able to take smaller pieces and easily compose them into larger pieces, which can themselves be composed.
Over the decades, the programming community has made great strides in this endeavor.
Each time we figure something out, however, and make a leap forward, we inevitably run into the next wall.

Our housing example is a decent reflection of where most programming is now: we have chunks of code—modules—and we can put them together pretty easily.
We have been improving our type systems and the ways we create data structures.
What wall are we facing now?

To return to our home-building system, we've solved the problem of assembling rooms, but adding functionality to those rooms is quite difficult.
If we decide we want electricity in a closet, we have to tear up the walls and insert electrical conduits.
To add a vent to a kitchen we must tunnel up through the building to the roof.
Producing a laundry room is very challenging because plumbing usually runs through the concrete foundation.
We can assemble rooms, but if we want a room to do anything interesting, we must basically remodel the house.

In the software world, we have pretty good ways to assemble software components.
For example, you can create (or reuse) a component that gets information from a server, processes it and then displays it.
What happens if you put this component into use and then discover that the server is flaky?
Perhaps the server occasionally drops requests, or takes too long.
There are different strategies for this: retrying, backoff, querying other servers, etc.
The problem is that, like the home-building system, you must go into your module and rebuild it.
This takes time and effort and it complicates your code.
What we'd really like to do is just attach our new functionality to the existing code without rewriting it.

In the home-building system, what if each room contains a channel, and when you assemble rooms, the channels match up?
Now if you want plumbing, electricity, venting, network cabling, etc., you just run it through the channel.
New features can be added to rooms without rebuilding the house.

This book introduces *effect systems*, which allow you to do the same thing for software as we have done for home-building:
  Add features without rewriting the software.

## What's Stopping Us?

The "wall" that we've run into here is that we don't have the channel that the home-building system does.
It's hard to imagine what that channel would look like, or how it behaves.
To get there we must start with some basic issues.
A dominant issue is *predictability*.

Consider a simple function:

```scala
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

```scala
val rand = new scala.util.Random

def fu(a: Int, b: Int): Int = 
  a + b + rand.nextInt()
```

Not surprisingly, adding a random number to the result takes us from predictable to unpredictable.
`fu` never fails and always produces an `Int`, but if you call it twice with the same inputs, you get different outputs.

These unpredictable elements are called *Effects*.

## Managing Effects

What if we could control an Effect by putting it in a kind of box?
Instead of using `scala.util.Random`, we can make our own random number generator:

```scala
val rand = new ControlledRandom

def fc(a: Int, b: Int): Int = 
  a + b + rand.nextInt()
```

`ControlledRandom` presumably contains `scala.util.Random`, but it could contain anything else. 
For example, we could swap in a custom generator to produce controlled results for testing `fc`.

Through `ControlledRandom`, we control the output of `rand`. 
If we provide a certain set of inputs, we can predict the output.
Once again, the function is pure.

It is predictable, so a pure function is testable.
We achieve this by *managing* the effect.
Managing the effect means that we not only control *what* results are produced by `rand`, we also control *when* those results are produced. 
The control of *when* is called *deferred execution*.
Deferred execution is a foundation that allows us to easily attach functionality to an existing program.

Consider the module that gets data from a server, processes it and then displays it.
That code is executed all at once.
If the server we’re trying to connect to is flaky and we’d like to add a retry, we don’t have direct access to the call to the server, which is hidden behind a wall of code.

Now let’s treat the call to the server as an Effect.
We manage it by putting a “box” around it like we did with `ControlledRandom`.
Because the execution of that Effect is deferred, we have the option of attaching the retry (or some other strategy) directly to that Effect, when it is executed.

This still sounds complicated, and hard to imagine how to write this kind of code.
That’s why we have *Effect Management* systems to provide the structure for you.
Effect Management enables us to add almost any functionality to a program.

Now it sounds a bit too simple: Just add an Effect Management system!
It still requires a significant shift in the way you think about programming.
Also, an Effect Management system includes libraries

## From Unpredictable to Predictable

Systems built from unpredictable parts can have predictable behavior.
This might sound far-fetched or even impossible.

Most existing languages are built for rapid development.
You create a system as quickly as possible,
  then begin isolating areas of failure,
  finding and fixing bugs until the system is tolerable and can be delivered.
Throughout the lifetime of the system,
  bugs are regularly discovered and fixed.
There is no realistic expectation that you will ever achieve a completely bug-free system,
  just one that seems to work well enough to meet the requirements.
This is the reality programmers have come to accept.

If each piece of a traditional system is unpredictable,
  when you combine these pieces you get a multiplicative effect
  -- the resulting parts are significantly less predictable than their component pieces.

What if we could change our thinking around the problem of building software systems?
Imagine building small pieces that can each be reasoned about and made predictable.
Now suppose there is a way to combine these predictable pieces to make larger parts that are just as predictable.
Each time you combine smaller parts to create a larger part, the result inherits the predictability of its components.
Instead of multiplying unpredictability, you maintain predictability.
The resulting system is as predictable as any of its components.

This is what *Functional Programming* together with *Effect Systems* can achieve.
This is what we want to teach you in this book.

The biggest impact on you as a programmer is the requirement for patience.
With most languages,
  the first thing you want to do is figure out how to write "Hello, World!",
  then start accumulating the other language features as standalone concepts.
In Functional Programming we start by examining the impact of each concept on predictability.
We then combine the smaller concepts, ensuring predictability at each step.

A predictable system isolates parts that are always the same
  (called pure functions)
  from the parts that are unpredictable
  (Effects).

## Dealing With Unpredictability

Any real program has to interact with things outside the programmer's control.
All external systems are unpredictable.

Building systems that are predictable requires isolating and managing the unpredictable parts.
An approach that programmers may use to handle this is to isolate the parts of the program which use external systems.
By isolating these parts, programmers can handle the unpredictable parts in more predictable ways.

The interactions with external systems can be defined in terms of "Effects".
Effects create a divistion between parts of a program that interact with external systems and parts that don't.

For example, a program that displays the current date requires something that actually knows the current date.
That program must talk to an external system, usually just the operating system, to get this information.
Such programs are unpredictable because the programmer has no control over what that external system will say or do.

## What is an Effect?

An *Effect* is an interaction, often with an outside system, that is inherently unpredictable.

Anytime you run an Effect, you change the world and cannot undo that change:
- If you 3D-print a figurine, you cannot reclaim that material.
- Once you send a Tweet, you can delete it but people might have already read it.
- Even if you provide database `DELETE` statements paired with `INSERT` statements, it must still be considered Effectful.
  Another program might read your data before you delete it,
  or a database trigger might activate during an `INSERT`.

Once a program runs an Effect, the impact is out of our control.

We must also assume that running an Effect modifies an external system.
As a real-life example, just saying "You are getting a raise" creates an Effect that may not be reversible.

Effects that only read data from external systems are also unpredictable; for example, getting a random number.

There are many types of Effects:

- Accepting user input
- Reading from a file
- Getting the current time from the system clock
- Generating a random number
- Displaying on the screen
- Writing to a file
- Mutating a variable
- Saving to a database
- And more...

These can have domain specific forms:

- Getting the current price of a stock
- Detecting the electrical current from a pacemaker
- Checking the temperature of a nuclear reactor
- 3D printing a model
- Triggering an alarm
- Sensing slippage in an anti-lock braking system
- Stabilizing an airplane
- Detonating explosives

All of these are unpredictable.

## `Unit` and Effects

`Unit` can be viewed as the bare minimum of Effect tracking.

Consider a function

```scala 3 mdoc
import zio.*
import zio.direct.*

def saveInformation(info: String): Unit =
  ???
```

The type of this function is `String => Unit`.
`Unit` is the blunt instrument that indicates an Effectful function.

When a function returns `Unit`, you don't call it to produce a result value.
When we see a `Unit` return type, we know that a side-effect must happen when we call that function---there's no other reason to do it.

If a function has no parameters, this is the same as having a `Unit` argument. 
This means an Effect is used to *produce* the result.

## Effect Systems

Given that Effects are unpredictable, we can utilize operations from an Effect System to manage the unpredictability.
Effect Systems are designed to make these operations easy.
For example, any Effect can use a `timeout` to control the Effect's maximum duration.

Applying these operations starts to feel like a superpower, and that's what we show in the next chapter.
