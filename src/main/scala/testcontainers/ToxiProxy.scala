package testcontainers

import eu.rekawek.toxiproxy.model.ToxicDirection
import org.testcontainers.containers.{
  GenericContainer,
  KafkaContainer,
  MockServerContainer,
  Network,
  ToxiproxyContainer
}
import org.testcontainers.utility.DockerImageName
import zio.{Has, ZIO, ZLayer}

import scala.jdk.CollectionConverters.*

object ToxyProxyContainerZ:
  val TOXIPROXY_NETWORK_ALIAS = "toxiproxy"
  def apply(
      network: Network
  ): ToxiproxyContainer =
    val TOXIPROXY_IMAGE =
      DockerImageName
        .parse("shopify/toxiproxy:2.1.0");
    // Toxiproxy container, which will be used as
    // a TCP proxy
    new ToxiproxyContainer(TOXIPROXY_IMAGE)
      .nn
      .withNetwork(network)
      .nn
      .withNetworkAliases(
        TOXIPROXY_NETWORK_ALIAS
      )
      .nn

  def construct(): ZLayer[Has[
    Network
  ] & Has[NetworkAwareness], Throwable, Has[
    ToxiproxyContainer
  ]] =
    for
      network <-
        ZLayer.service[Network].map(_.get)
      container = apply(network)
      res <-
        GenericInteractionsZ
          .manageWithInitialization(
            container,
            "toxi"
          )
          .toLayer
    yield res

  def use(
      toxiproxyContainer: ToxiproxyContainer,
      mockServerContainer: MockServerContainer
  ) =

    println("Using")
    val proxy
        : ToxiproxyContainer.ContainerProxy =
      toxiproxyContainer
        .getProxy(
          mockServerContainer,
          mockServerContainer.getServerPort.nn
//          mockServerContainer
//            .getExposedPorts
//            .nn
//            .asScala
//            .head
        )
        .nn

    proxy
      .toxics()
      .nn
      .latency(
        "latency",
        ToxicDirection.DOWNSTREAM,
        1_100
      )

    println("Used")
    proxy.getProxyPort.nn
  end use
end ToxyProxyContainerZ
