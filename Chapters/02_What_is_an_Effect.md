# What is an Effect?

08:07 AM January 13, 2018: Televisions, Radios, and Cell Phones across Hawaii suddenly flash an alert.

C> "BALLISTIC MISSILE INBOUND THREAT TO HAWAII. SEEK IMMEDIATE SHELTER. THIS IS NOT A DRILL"

Local communities sound alarms.

Calls to 911 jam the phone lines; panicked internet searches overwhelm data networks.

Hundreds of students sprint from their classrooms to fallout shelters.

Parents say goodbye to their children.

### Unwinding the Chaos
Thankfully, no missiles had been launched that day.
An employee at the Hawaii Emergency Management Agency had accidentally sent this warning, during what should have been a quiet system test.

TODO {{ Properly cite this: https://www.washingtonpost.com/news/post-nation/wp/2018/01/14/hawaii-missile-alert-how-one-employee-pushed-the-wrong-button-and-caused-a-wave-of-panic/}}

From the Washington Post

C> "He clicked the button to send out an actual notification on Hawaii's emergency alert interface during what was intended to be a test of the state's ballistic missile preparations computer program."
C> The employee was prompted to choose between the options "test missile alert" and "missile alert", had selected the latter, initiating the alert sent out across the state.

This is the actual screen that the employee was interacting with that day:

![](images/HawaiiAlertSystem.jpg)

Ignore the disastrous user-interface; it would take a separate book to fully deconstruct that.
The relevant flaw here is that both the Live and Test effects were available side-by-side in the running application.

The effects of this system were:
  - Sending alert messages to Cell Phones
  - Playing alert messages on Radio frequencies
  - Displaying alert messages on Television stations

## Effects - Definition

An `Effect` is the term for any computational interaction with the world outside your CPU.
There are an infinite number of `Effects` that might need to be modeled in an application.
Unauthoritatively, the authors of this book consider them in these categories.

- Observing the World
- Changing the World

TODO {{Explain these Effects: Optionality, Failure, Asynchronicity, Blocking}}

## Observing the World

Observation can be very basic, such as:

- Accepting user input from the `Console`
- Getting the current time from the system `Clock`
- Taking the output of a `Random` number generator

ZIO provides built-in `Services` for many of these common programming tasks.
We will examine these pieces in detail, contrasting them with historic approaches, and highlighting some of their benefits.

However, ZIO-core does not try to cover all every possible situation.
Observation can also be arbitrarily complex and domain-specific:

- Sensing slippage in an anti-lock braking system
- Getting the current price of a stock
- Detecting the current passing through your heart for a pace-maker
- Checking the temperature of a nuclear reactor

We will explore situations like this in our chapter of more complex scenarios

## Changing the World

Mirroring observations, changes can be basic:

- Printing to the `Console`
- 3D printing a model
- Writing to a file
- Mutating a variable
- Saving to a `Database`

They can be advanced:

- Triggering an alarm
- Stabilizing an airplane
- Detonating explosives

## No take-backs
TODO {{ Less idiom-y title }}

One important aspect of effects is that, generally, they cannot be undone.
If you 3D-printed a figurine, you cannot reclaim that material.
If you sent a Tweet, people might have read it, and no amount of deleting will remove that information from their minds.

It is possible to undo special cases- eg. provide Database `DELETE` statements paired with your `INSERT` statements.
However, even in this relatively simple case, a Database trigger might have been activated on `INSERT` that is completely hidden from you.

## Effects VS Side-Effects
`Effects` and `Side-Effects` are not slight verbiage preferences.
They are a fundamentally different way of modeling our programs.

`Side-effecting` code observes or changes the world in some way that is not apparent in the type signature.
`Effectful` code will signal this in the type signature.
If your 

## The Advent of ZIO
