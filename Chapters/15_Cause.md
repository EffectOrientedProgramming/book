# Cause

Consider an Evolutionary example, where a `Cause` allows us to track 
MutationExceptions throughout a length process

Cause will track all errors that occur in an application, regardless of concurrency and parallelism

Cause allows you to aggregate multiple errors of the same type

&&/Both represents parallel failures
++/Then represents sequential failures

Cause.die will show you the line that failed, because it requires a throwable
Cause.fail will not, because it can be any arbitrary type
