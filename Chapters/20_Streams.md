# Streams

If you want to get all items in a defined range, eg `July 1st - July 3rd`, then you might not need a `Stream`.
However, if you want to get all items in a range that is *not* bounded, eg `From July 1st onward`, then you need a `Stream`.

## UI Interactions
UI events are a great use-case for streams.
When you present a UI to a user, it is impossible to know how they will interact with it.
They might click a few buttons, and finish in a few minutes.
They might *never* interact with it at all.
They might leave the page open for days, only occasionally interacting with it.
It is impossible to run a function at any point in time that returns a `List[Event]` that represents all interactions, because it is an unbounded, ongoing concept.

In addition, it's unlikely that you want to hold _all_ of UI events at one time.
It is enough to process them individually, or in small batches, as they occur.

TODO Prose