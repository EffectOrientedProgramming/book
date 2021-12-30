# Resources

Resources are finite / large overhead allocatable pools of things:
 - Connections
 - File handles
 - Player Slots (Rule based or Fractional Computational Power - The usefulness of the game degrades at some scale)
 - STM? Is it based on ZManaged?

Connection with dining philosophers

Externalizes the resource management so that the logic that acts on the resource can be reused, refactored, composed.

Assembly of resources works the same as a single resource. If a resource is more than 1 resource, the logic acting on any / all resources doesn't have to know what cleanup.  Similarly, the logic is unconcerned with the ability for all needed resources to be available.  Logic is only ever to be applied when all resources are available.

