# Introduction

08:07 AM January 13, 2018

Televisions, Radios, and Cell Phones across Hawaii suddenly flash an alert:

C> "BALLISTIC MISSILE INBOUND THREAT TO HAWAII. SEEK IMMEDIATE SHELTER. THIS IS NOT A DRILL"

Local communities sound alarms.

Calls to 911 jam the phone lines.

Panicked internet searches overwhelm data networks.

Hundreds of students sprint from their classrooms to fallout shelters.

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
The fatal flaw was that both "live" and "test" alerts were available in the running application.
A safe system makes these behaviors mutually exclusive.

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
And consider security; all the applications you use that are constantly being updated with security patches (what about those that aren't? Are they that good, or is security being ignored?).

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

A reliable system does not break.

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
Instead of multiplying unreliability, you combine reliability.
The resulting system is as reliable as any of its components.

This is what *functional programming* together with *effects management* can achieve.
This is what we want to teach you in this book.

The biggest impact on you as a programmer is the requirement for patience.
With most languages, the first thing you want to do is figure out how to write "Hello, World!", then start accumulating the other language features as standalone concepts.
In functional programming we start by examining the impact of each concept on reliability.
We then combine the smaller concepts, ensuring reliability at each step.

A reliable system isolates parts that are always the same (pure functions) from the parts that can change (effects).
This mathematical rigor produces a reliable system.

It can seem like a painfully long process before you begin writing working programs.
Most of us are used to the more immediate feedback and satisfaction of getting something working, so this can be challenging.
But would you rather create an unreliable system quickly?
We assume you are reading this book because you do not.

## What is an Effect?

An *effect* is the term for any computational interaction with the world outside your CPU.
There are an infinite number of effects that might need to be modeled in an application.
We consider these categories:

- Observing the World
- Changing the World

TODO {{Explain these Effects: Optionality, Failure, Asynchronicity, Blocking}}

### Observing the World

Observation can be very basic, such as:

- Accepting user input from the console
- Getting the current time from the system clock
- Taking the output of a random number generator

ZIO provides built-in services for many of these common programming tasks.
We will examine these pieces in detail, contrasting them with historic approaches, and highlighting some of their benefits.

ZIO does not try to cover every possibility.
Observations can also be arbitrarily complex and domain-specific:

- Sensing slippage in an anti-lock braking system
- Getting the current price of a stock
- Detecting the current passing through your heart from a pacemaker
- Checking the temperature of a nuclear reactor

We explore similar scenarios throughout the book.

### Changing the World

Mirroring observations, changes can be basic:

- Displaying on the console
- Writing to a file
- Mutating a variable
- Saving to a database

They can be advanced:

- 3D printing a model
- Triggering an alarm
- Stabilizing an airplane
- Detonating explosives

## You Can't Undo an Effect

One important aspect of effects is that, generally, they cannot be undone.
If you 3D-print a figurine, you cannot reclaim that material.
Once you send a Tweet, you can delete it but people might have already read it.

It is possible to undo special cases.
For example, you can provide database `DELETE` statements paired with your `INSERT` statements.
However, even in this relatively simple case, a database trigger might have been activated on an `INSERT` that is completely hidden from you.

## Effects VS Side-Effects

The distinction between the terms *effects* and *side-effects* are important.
Each represents a fundamentally different way of modeling a program.

Side-effecting code observes or changes the world in some way that is not apparent in the type signature.
Effectful code signals this in the type signature.
If your

## The Advent of ZIO


## Who this book is for

* Your background
* What to expect

This is not a comprehensive Scala 3 book.
For that we recommend [Programming in Scala 3](https://www.TODO.com).
We expect the kind of basic programming knowledge that allows you to effectively guess at the meaning of basic Scala code.
We explain more complex Scala syntax as it appears in the book.
However, we avoid the use of complex Scala syntax, as our goal is to teach ZIO in as simple a fashion as possible.

## How to use this book


### Individual Study

### Teaching Situations

## Getting Started

All the examples and exercise solutions are contained in the book's [Github Repository](https://github.com/EffectOrientedProgramming/EOPCode).
The README for this repository contains thorough, step-by-step instructions for setting up your computer to compile and run the examples and exercise solutions.

## Acknowledgements

Kit Langton, for being a kindred spirit in software interests and inspiring contributor to the open source world.

Wyett Considine, for being an enthusiastic intern and initial audience.
