"An Iterator is the fundamental imperative representation of collections of zero or more and potentially infinitely many values. 
Similarly, ZStream is the fundamental functional representation of collections of one or more and potentially infinitely many effectual values."
- Zionomicon

"you can always operate at the level of chunks if you want to. 
But the philosophy of ZIO Stream is that the user should have to manually deal with chunking only as an optimization and 
that in most cases the framework should automatically “do the right thing” with regard to chunking."
- Zionomicon

"in a streaming application implementing a sorted operator would require not only waiting to emit any value until the original stream had terminated, 
which might never happen, but also buffering a potentially unlimited number of prior values, creating a potential memory leak."
- Zionomicon

"""Here are some common collection operators that you can use to transform streams:
    • collect - map and filter stream values at the same time
    • collectWhile - transform stream values as long as the specified partial function is defined
    • concat - pull from the specified stream after this stream is done
    • drop - drop the first n values from the stream
    • dropUntil - drop values from the stream until the specified predicate is true
    • dropWhile - drop values from the stream while the specified predicate is true
    • filter - retain only values satisfying the specified predicate
    • filterNot - drop all values satisfying the specified predicate
    • flatMap - create a new stream for each value and flatten them to a single stream
    • map - transform stream values with the specified function
    • mapAccum - transform stream values with the specified stateful function 
    • scan - fold over the stream values and emit each intermediate result in a new stream
        Bill note - Naively, this one seems dangerous
    • take - take the first n values from the stream
    • takeRight - take the last n values from the stream
    • takeUntil - take values from the stream until the specified predicate is true
    • takeWhile - take values from the stream while the specified predicate is true
    • zip - combine two streams point wise
"""
- Zionomicon

TODO - Should we talk about unfold?
"In addition to the basic variant of unfold there is also an effectual variant, unfoldZIO, 
which allows performing an effect in the state transformation function. 
This allows describing many types of streams, for example reading incrementally from a data source 
while maintaining some cursor that represents where we currently are in reading from the data source."
- Zionomicon

"the signature of the foreach method on ZStream returns a ZIO effect and so it is safe to use. Using this, we can write code like:

    val effect: ZIO[Any, Nothing, Unit] = for {
        x <- ZStream(1, 2)
        y <- ZStream(x, x + 3)
    } Console.printLine((x, y).toString).orDie
This now just describes running these two streams and printing the values they produce to the console."
- Zionomicon
