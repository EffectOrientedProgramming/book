# What is an Effect?

08:07 AM January 13, 2018

Televisions, Radios, and Cell Phones across Hawaii suddenly flash an alert:

C> "BALLISTIC MISSILE INBOUND THREAT TO HAWAII. SEEK IMMEDIATE SHELTER. THIS IS NOT A DRILL"

Local communities sound alarms.

Calls to 911 jam the phone lines.

Panicked internet searches overwhelm data networks.

Hundreds of students sprint from their classrooms to fallout shelters.

Parents say goodbye to their children.

### Untangling the Chaos
Thankfully, no missiles had been launched that day.
An employee at the Hawaii Emergency Management Agency had accidentally sent this warning, during what should have been a quiet system test.

From the Washington Post: [Citation](15_citations.md#hawaii-alert)

C> "He clicked the button to send out an actual notification on Hawaii's emergency alert interface during what was intended to be a test of the state's ballistic missile preparations computer program."
C> The employee was prompted to choose between the options "test missile alert" and "missile alert", had selected the latter, initiating the alert sent out across the state.

This is the actual control screen in the system:

![](images/HawaiiAlertSystem.jpg)

TODO {{ How much to berate the UI? Too much distracts from the ultimate point }}
{{ Perhaps just mention the numerous other issues including the UI but don't spend much time. Most readers will be familiar with UI failings }}
This cluster of inconsistently named links made mistakes likely.
Basic changes would have drastically simplified correct use of the alerts.
Imagine what earlier mishaps moved "False Alarm" to the top of the list.

However, we believe that the system was doomed long before the interface was created.
The fatal flaw was that both the Live and Test alerts were available in the running application.
We want to write systems in which these behaviors are mutually exclusive.
{{ This suggests that "systems in which these behaviors are mutually exclusive" is the essence. If not, we might say "one of the things we want to do" or something}}


TODO {{ "Tease" the Effects here, or only discuss them in the following sections?}}
The Effects of this system were:

  - Sending messages to Cell Phones
  - Playing warnings on Radio frequencies
  - Displaying banners on Television stations

## Effects - Definition

An `Effect` is the term for any computational interaction with the world outside your CPU.
There are an infinite number of `Effects` that might need to be modeled in an application.
Unauthoritatively, the authors of this book consider them in these categories.
{{ Tough phrase, because authoritative technically means "from an author" }}

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
- Writing to a file
- Mutating a variable
- Saving to a `Database`

They can be advanced:

- 3D printing a model
- Triggering an alarm
- Stabilizing an airplane
- Detonating explosives

## No take-backs
TODO {{ Less idiom-y title }}
{{ Possibly but it's also very evocative }}

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
