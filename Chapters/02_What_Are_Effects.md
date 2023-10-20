# What Are Effects

Any real program has to interact with things outside the programmer's control.

All external systems are unpredictable.

One of the biggest challenges in building systems that are more predictable / reliable / ??? is to isolate and manage the unpredictable parts.

An approach that programmers may use to handle this is to delineate the parts of the program which use external systems.

By delineating them, programmers then have tools to handle the unpredictable parts in more predictable ways.

The interactions with external systems can be defined in terms of "Effects" which create a delineation between the parts of a program that interact with external systems and those that don't.

For example, a program that displays the current date requires something that actually knows the current date.

The program needs to talk to an external system (maybe just the operating system) to get this information.

These programs are unpredictable because the programmer has no control over what that external system will say or do.

Effect Oriented systems allow us to apply strategies to mitigate the unpredictability of using external systems.

Effects can not be un-done.

Once a program has communicated with an external system, (i.e. executed an Effect), everything that happens on that external systems is out of the program's control.

(analogies on human communication)

The Effect is not what happens on the external system because there is no way to know the actual impact of what the program caused by the communication.
