# What Are Effects

TODO: General idea of what an effect is

The Effect is not what happens on the external system because there is no way to know the actual impact of what the program caused by the communication.

## Dealing with unpredictability

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

## Effects can not be un-done

Once a program has communicated with an external system, (i.e. executed an Effect), everything that happens on that external systems is out of the program's control.

(analogies on human communication)
Imagine that a friend recently stayed in your home.
3 days after they leave, you realize that you are missing some money that had been stored in the guestroom.
Now you have a dilemma - do you ask them if they took the money?
Simply by asking, you could permanently change, or even end, your relationship with this person.
They could immediately admit fault, and ask for forgiveness.
Now you know that they are capable of stealing from you - will you ever trust them in your home again?
They could angrily deny the accusation, and resent you for making it.
Or the conversation could go in a million different other ways that are impossible to predict.
We know one thing for certain - you will never be able to un-ask that question.
Even if you ultimately grow closer with this person after navigating this situation, you can't go back to a world where you never asked. 
Regardless of any apology and forgiveness, your relationship is now different.

## Effects Defined as Data

TODO

One approach to defining effects is...

The effects have not been executed when defined.
