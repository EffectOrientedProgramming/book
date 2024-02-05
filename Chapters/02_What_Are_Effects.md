# What Are Effects

## Introduction

TODO: Combine with "Reliability" ?

08:07 AM January 13, 2018

Televisions, Radios, and Cell Phones across Hawaii suddenly flash an alert:

C> "BALLISTIC MISSILE INBOUND THREAT TO HAWAII. SEEK IMMEDIATE SHELTER. THIS IS NOT A DRILL"

Local communities sound alarms.

Calls to 911 jam the phone lines.

Panicked internet searches overwhelm data networks.

Students sprint from classrooms to fallout shelters.

Parents say goodbye to their children.

## Untangling the Chaos

Thankfully, no missiles were launched that day.

During what should have been a quiet system test, an employee at the Hawaii Emergency Management Agency accidentally pushed the wrong button.
From the [Washington Post](https://www.washingtonpost.com/news/post-nation/wp/2018/01/14/hawaii-missile-alert-how-one-employee-pushed-the-wrong-button-and-caused-a-wave-of-panic/):

C> "He clicked the button to send out an actual notification on Hawaii's emergency alert interface during what was intended to be a test of the state's ballistic missile preparations computer program."
C> The employee was prompted to choose between the options "test missile alert" and "missile alert", had selected the latter, initiating the alert sent out across the state.

Here is the system's control screen:

![](images/HawaiiAlertSystem.jpg)

This cluster of inconsistently named links increased the likelihood of mistakes.
Basic changes would drastically simplify proper use of the alerts.
Imagine the earlier mishaps that moved "False Alarm" to the top of the list.

We believe the system was doomed long before the interface was created.

The effects of this system were:

- Sending messages to Cell Phones
- Playing warnings on Radio frequencies
- Displaying banners on Television stations

## The State of Software

There are many other examples of carefully-built software systems failing disastrously:

- The Ariane 5 rocket self-destructed on 4 June 1996 because of a malfunction
  in the control software (the program tried to stuff a 64-bit number into a
  16-bit space).

- The American Northeast Power Blackout, August 14 2003.

- The NASA Mars Climate Orbiter, September 23, 1999. The orbiter was programmed
  for metric but ground control software used non-metric English.

The list goes on; just search for something like "Famous Software Failures" to see more.
And consider security- all the applications you use that are constantly being updated with security patches.
What about those that aren't?
Are they that good, or is security being ignored?

How did things get so bad?

## The Software Crisis

In the 70's and 80's, the idea of the *Software Crisis* emerged.
This can be summarized as: "We can't create software fast enough."
One of the most popular attempts to solve this problem was *Structured Analysis & Design*, which was a way to understand a problem and design a solution using existing imperative languages.

The real problem that Structured Analysis & Design set out to solve was big monolithic pieces of code.
When one programmer was able to solve the entire problem, the structure of the program didn't matter as much.
But as software needs grew, this approach didn't scale.
In particular, you couldn't finish a project more quickly by adding more programmers, because there wasn't a way to hand off portions of a program to multiple programmers.
To do that, teams needed some way to break down the complexity of the program into individual functions---functions that might someday be reused.
This was seen as the reason for the Software Crisis.

Structured Analysis was an attempt to discover the individual functions in a program.
But it was a top-down approach, and it assumed these functions could be determined before any code is written.
Structured Analysis & Design continued the approach of "big up-front design."
The analyst produced the structure, and then the programmers implemented it.

Experienced programmers know that a design that cannot evolve during development is doomed to failure: both programmers and stakeholders learn things during development.
You discover much of your structure *as* you're building the program, and not on a whiteboard.
Building a program reveals things you didn't know were important when you designed the solution.

From this book's perspective, the most fundamental problem with Structured Analysis & Design was that it only paid lip service to the idea of reliability.
There was nothing about reliability truly integrated into Structured Analysis & Design.

Structured Analysis & Design was motivated by a business problem: "how do we create software faster?"
Virtually every language that came out in its aftermath focused on development speed.
Not reliability.
So we produced a lot of languages to quickly create unreliable software.

## Reliability

A reliable system does not break. // TODO Discuss

If you've been programming for a while, this sounds far-fetched or even impossible.

Most existing languages are built for rapid development.
You create a system as quickly as possible, then begin isolating areas of failure, finding and fixing bugs until the system is tolerable and can be delivered.
Throughout the lifetime of the system, bugs are regularly discovered and fixed.
There is no realistic expectation that you will ever achieve a completely bug-free system, just one that seems to work well enough to meet the requirements.
This is the reality programmers have come to accept.

If each piece of a traditional system is unreliable, when you combine these pieces you get a multiplicative effect -- the resulting parts are significantly less reliable than their component pieces.

What if we could change our thinking around the problem of building software systems?
Imagine building small pieces that can each be reasoned about and made rock-solid.
Now suppose there is a way to combine these reliable pieces to make bigger parts that are just as reliable.
Each time you combine smaller parts to create a bigger part, the result inherits the reliability of its components.
Instead of multiplying unreliability, you maintain reliability.
The resulting system is as reliable as any of its components.

This is what *functional programming* together with *effects management* can achieve.
This is what we want to teach you in this book.

The biggest impact on you as a programmer is the requirement for patience.
With most languages, the first thing you want to do is figure out how to write "Hello, World!", then start accumulating the other language features as standalone concepts.
In functional programming we start by examining the impact of each concept on reliability.
We then combine the smaller concepts, ensuring reliability at each step.

A reliable system isolates parts that are always the same (pure functions) from the parts that can change (effects).
This mathematical rigor produces a reliable system.

Some aspects of writing code in this style might seem onerous.
Most of us are used to the more immediate feedback and satisfaction of getting something working, so this can be challenging.
But would you rather create an unreliable system quickly?
We assume you are reading this book because you do not.


The Effect is not what happens on the external system because there is no way to know the actual impact of what the program caused by the communication.

## Dealing with unpredictability

Any real program has to interact with things outside the programmer's control.

All external systems are unpredictable.

Building systems that are reliable requires isolating and managing the unpredictable parts.

An approach that programmers may use to handle this is to delineate the parts of the program which use external systems.

By delineating them, programmers then have tools to handle the unpredictable parts in more predictable ways.

The interactions with external systems can be defined in terms of "Effects" which create a delineation between the parts of a program that interact with external systems and those that don't.

For example, a program that displays the current date requires something that actually knows the current date.

The program needs to talk to an external system (maybe just the operating system) to get this information.

These programs are unpredictable because the programmer has no control over what that external system will say or do.

Effect Oriented systems allow us to apply strategies to mitigate the unpredictability of using external systems.

## Effects can not be un-done

Once a program has communicated with an external system, 
  (i.e. executed an Effect),
  everything that happens on that external systems is out of the program's control.

(analogies on human communication)
Imagine that a friend recently stayed in your home.
3 days after they leave,
  you realize that you are missing some money that had been stored in the guestroom.
Now you have a dilemma -
  do you ask them if they took the money?
Simply by asking,
  you could permanently change, 
    or even end, 
    your relationship with this person.
They could immediately admit fault,
  and ask for forgiveness.
Now you know that they are capable of stealing from you -
  will you ever trust them in your home again?
They could angrily deny the accusation,
  and resent you for making it.
Or the conversation could go in a million different other ways that are impossible to predict.
We know one thing for certain - 
  you will never be able to un-ask that question.
Even if you ultimately grow closer with this person after navigating this situation,
  you can't go back to a world where you never asked. 
Regardless of any apology and forgiveness, 
  your relationship is now different.

## What is an Effect?

An *effect* is an interaction with the world outside your CPU.
An application might generate any number of effects, which fall into two categories:

- Observing the World
- Changing the World

Effects cannot be undone.
If you 3D-print a figurine, you cannot reclaim that material.
Once you send a Tweet, you can delete it but people might have already read it.
Even if you provide database `DELETE` statements paired with `INSERT` statements, it must still be considered effectful.
Another program might read your data before you delete it, 
  or a database trigger might activate during an `INSERT`.

TODO {{Explain: Optionality, Asynchronicity, Blocking -- In a later chapter. }}


### Observing the World

Observation can be very basic:

- Accepting user input from the console
- Getting the current time from the system clock
- Taking the output of a random number generator

Observations can also be complex and domain-specific:

- Sensing slippage in an anti-lock braking system
- Getting the current price of a stock
- Detecting the current from a pacemaker
- Checking the temperature of a nuclear reactor

We explore similar scenarios throughout the book.

### Changing the World

Just as with observations, changes can be basic:

- Displaying on the console
- Writing to a file
- Mutating a variable
- Saving to a database

They can be advanced:

- 3D printing a model
- Triggering an alarm
- Stabilizing an airplane
- Detonating explosives

## ZIOs are not their result.

### Effects Defined as Data

One approach to defining effects is...

The effects have not been executed when defined.

A common mistake when starting with ZIO is to return ZIO instances themselves rather than running them to produce a result.

This is a mistake because ZIO's are not their result, they are descriptions of effects that produce the result.
The `ZIO` instance only describes something *to be* done.

ZIOs are not automatically executed.
To actually run a ZIO, your program must take the data types and interpret / run them, executing the logic .
The user must determine when/where that happens.

Consider the `Option` type in the standard library.
An `Option` _might_ have a value inside of it, but you can't safely assume that it does.
Similarly, a `ZIO` _might_ produce a value, but you have to run it to find out.

You can think of them as recipes for producing a value.
You don't want to return a recipe from a function, you can only return a value.
If it is your friend's birthday, they want a cake, not a list of instructions about mixing ingredients and baking.


## The Interpreter

Scala compiles code to JVM bytecodes,
An interpreter steps through and executes your code, much like the JVM interprets JVM bytecodes.
The interpreter is the hidden piece understands so much more about the meaning of your code.
This includes the ability to decide what to run concurrently and how to invisibly tune that concurrency--all at runtime.
The interpreter is responsible for deciding when to context-switch between tasks, and is able to do this because it understands the ZIO code that it's executing.

The interpreter is also the mechanism that evaluates the various effects described in the generic type parameters for each ZIO object.

The reason we have the `defer` directive(method?) in zio-direct is to indicate that this code will be evaluated by the interpreter later.

