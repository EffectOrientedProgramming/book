Your program displays 2 sections:
    Summary
        -Time range
        -totalNumberOfTransactions 
        -All Participants
    Details
        - List[Transaction]
        
Show how these can be out of sync with unprincipled `Clock` access

`.now()`

How often it is overlooked/minimized
"Race Condition" vs "race operation"
Example possibilities
    - Progress bar
    - query(largeRange) followed by query(smallRange), and getting new results in the 2nd call