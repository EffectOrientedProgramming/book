# Network

Talking to the network is an effect. 
It can fail. 
We must handle these situations.

```scala mdoc
import java.net.URL
val url1 =
  new URL("https://www.google.com")
val url2 =
  new URL("https://www.google.com")
assert(url1.equals(url2))
```

If we check the docs for this function, we find:
```java
/*
 * Two hosts are considered equivalent if both host names can be resolved
 * into the same IP addresses
 */
```
