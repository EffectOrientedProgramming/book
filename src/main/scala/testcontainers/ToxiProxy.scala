package testcontainers

import org.testcontainers.containers.ToxiproxyContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.containers.{
  GenericContainer,
  Network
}
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
