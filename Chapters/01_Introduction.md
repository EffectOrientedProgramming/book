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
An employee at the Hawaii Emergency Management Agency had accidentally sent this warning, during what should have been a quiet system test.

From the Washington Post: [Citation](15_citations.md#hawaii-alert)

C> "He clicked the button to send out an actual notification on Hawaii's emergency alert interface during what was intended to be a test of the state's ballistic missile preparations computer program."
C> The employee was prompted to choose between the options "test missile alert" and "missile alert", had selected the latter, initiating the alert sent out across the state.

This is the actual control screen in the system:

![](images/HawaiiAlertSystem.jpg)

This cluster of inconsistently named links increased the likelihood of mistakes.
Basic changes could drastically simplify proper use of the alerts.
Imagine what earlier mishaps moved "False Alarm" to the top of the list.

We believe the system was doomed long before the interface was created.
The fatal flaw was that both Live and Test alerts were available in the running application.
A safer system makes these behaviors mutually exclusive.

The effects of this system were:

  - Sending messages to Cell Phones
  - Playing warnings on Radio frequencies
  - Displaying banners on Television stations

## Reliability

A reliable system does not break.

If you've been programming for a while, this sounds far-fetched or even impossible.

Most existing languages are built for rapid development.
You create a system as quickly as possible, then begin isolating areas of failure, finding and fixing bugs until the system is tolerable and can be delivered.
Throughout the lifetime of the system, bugs are regularly discovered and fixed.
There is no realistic expectation that you will ever achieve a completely bug-free system, just one that seems to work well enough to meet the requirements.
This is the reality we have come to accept as programmers.

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
With most languages, the first thing you want to do is figure out how to write "Hello, World!" as quickly as possible, then start accumulating the other language features as standalone concepts.
In functional programming we start by examining the impact of each concept on reliability.
Eventually we combine the smaller concepts, again ensuring reliability at each step.
This mathematical rigor is what ultimately produces a reliable system.

This takes patience.

It can seem like a painfully long process before you can begin writing working programs. Most of us are used to the more
immediate feedback and satisfaction of getting something working, so this can be challenging. But would you rather
create an unreliable system quickly? Presumably you do not, otherwise you wouldn't be reading this book.

A reliable system isolates parts that are always the same (pure functions)
from the parts that can change (effects).  

## What is an Effect?

An *effect* is the term for any computational interaction with the world outside your CPU.
There are an infinite number of effects that might need to be modeled in an application.
The authors of this book consider them in these categories:

- Observing the World
- Changing the World

TODO {{Explain these Effects: Optionality, Failure, Asynchronicity, Blocking}}

## Observing the World

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

We explore scenarios like this throughout the book.

## Changing the World

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

It is possible to undo special cases. For example, you can provide database `DELETE` statements paired with your `INSERT` statements.
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

This is not a comprehensive Scala 3 book. For that we recommend [Programming in Scala 3](https://www.TODO.com). 
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

Wyett Considine, for being an enthusiastic intern, math consultant, and initial audience.

Kit Langton, for being a kindred spirit in software interests and inspiring contributor to the open source world.
