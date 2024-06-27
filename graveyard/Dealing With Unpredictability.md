## Dealing With Unpredictability

Any real program has to interact with things outside the programmer's control.
All external systems are unpredictable.

Building systems that are predictable requires isolating and managing the unpredictable parts.
An approach that programmers may use to handle this is to isolate the parts of the program which use external systems.
By isolating these parts, programmers can handle the unpredictable parts in more predictable ways.

The interactions with external systems can be defined in terms of "Effects".
Effects create a division between parts of a program that interact with external systems and parts that don't.
