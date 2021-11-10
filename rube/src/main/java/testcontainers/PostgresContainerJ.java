package testcontainers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.Network;


public class PostgresContainerJ extends PostgreSQLContainer< PostgresContainerJ >{ 
  public static PostgresContainerJ apply(
      String initScript,
      Network network
  ){
    return new PostgresContainerJ()
      .withInitScript(initScript)
      .withNetwork(network)
      .withNetworkAliases("postgres");
  }
}
    // ("postgres:13.1")
