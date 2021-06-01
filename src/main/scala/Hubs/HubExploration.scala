package Hubs

object HubExploration {
  import zio._

  trait Hub[A] {
    def publish(a: A): UIO[Boolean]
    def subscribe: ZManaged[Any, Nothing, Dequeue[A]]
  }

}
