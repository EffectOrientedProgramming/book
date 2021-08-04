# Network

```scala
import java.net.URL
val url1 = new URL("https://www.google.com")
val url2 = new URL("https://www.scala.com")
assert( url1.equals(url2) )

```

If we check the docs for this function, we fine:
```java
/*
 * Two hosts are considered equivalent if both host names can be resolved
 * into the same IP addresses
 */
```