package mdoc

import org.testcontainers.containers.GenericContainer

object GenericInteractions:

  def stupidFuckingMethodThatICannotDelete[
      T <: GenericContainer[T]
  ]() = ???