### Performance

## Hedging
### Why?
This technique snips off the most extreme end of the latency tail.

Determine the average response time for the top 50% of your requests.
If you make a call that does not get a response within this average delay, make an additional, identical request.
However, you do not give up on the 1st request, since it might respond immediately after sending the 2nd.
Instead, you want to race them and use the first response you get

To be clear - this technique will not reduce the latency of the fastest requests at all.
It only alleviates the pain of the slowest responses.

You have `1/n` chance of getting the worst case response time.
This approach turns that into a `1/n^2` chance.
The cost of this is only ~3% more total requests made. *Citations needed*

Further, if this is not enough to completely eliminate your extreme tail, you can employ the exact same technique once more.
Then, you end up with `1/n^3` chance of getting that worst performance.