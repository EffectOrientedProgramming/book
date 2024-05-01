# Introduction to Effects

Systems built from unpredictable parts can have predictable behavior.

If you've been programming for a while, this sounds far-fetched or even impossible.

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
  (effects).

## Dealing With Unpredictability

Any real program has to interact with things outside the programmer's control.
All external systems are unpredictable.

Building systems that are predictable requires isolating and managing the unpredictable parts.
An approach that programmers may use to handle this is to isolate the parts of the program which use external systems.
By isolating them,
  programmers then have tools to handle the unpredictable parts in more predictable ways.

The interactions with external systems can be defined in terms of "Effects".
Effects create a delineation between the parts of a program that interact with external systems and those that don't.

For example, a program that displays the current date requires something that actually knows the current date.
The program needs to talk to an external system 
  (maybe just the operating system)
  to get this information.
These programs are unpredictable because the programmer has no control over what that external system will say or do.

## What is an Effect?

An *Effect* is an interaction, often with an outside system, that is inherently unpredictable.

Anytime you run an Effect,
  you have changed the world and cannot go back.

If you 3D-print a figurine, you cannot reclaim that material.
Once you send a Tweet, you can delete it but people might have already read it.
Even if you provide database `DELETE` statements paired with `INSERT` statements, it must still be considered effectful.
Another program might read your data before you delete it,
or a database trigger might activate during an `INSERT`.

Once our program runs an Effect, the impact is out of our control.

We must assume that running an Effect modifies an external system.

As an example in real-life, just saying the words "You are getting a raise" creates an "effect" that may not be reversible.

Effects that only read data from external systems are also unpredictable.
For example, getting a random number from the system is usually unpredictable.

In systems there are many types of Effects, like:

- Accepting user input
- Reading from a file
- Getting the current time from the system clock
- Generating a random number
- Displaying on the screen
- Writing to a file
- Mutating a variable
- Saving to a database

These can also have domain specific forms, like:

- Getting the current price of a stock
- Detecting the current from a pacemaker
- Checking the temperature of a nuclear reactor
- 3D printing a model
- Triggering an alarm
- Sensing slippage in an anti-lock braking system
- Stabilizing an airplane
- Detonating explosives

All of these are unpredictable.

## Effect Systems Manage Unpredictability

Given that Effects are unpredictable, we can utilize operations from an Effect System to manage the unpredictability.
Effect Systems are designed to make these operations easy.
For example, any Effect can use a `timeout` to control the Effect's maximum duration.

Applying these operations starts to feel like a superpower.
