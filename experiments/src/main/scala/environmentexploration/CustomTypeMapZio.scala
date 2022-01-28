package environmentexploration

import scalaBasics.forComprehension

// trait TypeTag // TODO Or ClassTag?
// trait TypeInstance

// case class TypeMap(
//   typeMap: Map[TypeTag, TypeInstance]
// )

val superSimple: Int => String =
  env => s"Message \n" * env

case class CustomTypeMapZio[ENV, RESULT](
    run: ENV => RESULT
):
  def unsafeRun(env: ENV): RESULT = run(env)

  // The tuple here is a step towards the
  // full-featured TypeMap that ZIO uses
  def flatMap[ENV2, RESULT2](
      f: RESULT => CustomTypeMapZio[
        ENV2,
        RESULT2
      ]
  ): CustomTypeMapZio[(ENV, ENV2), RESULT2] =
    CustomTypeMapZio((env, env2) =>
      f(run(env)).run(env2)
    )

@main
def demoSingleEnvironmentInstance =
  val customTypeMapZio
      : CustomTypeMapZio[Int, String] =
    CustomTypeMapZio(env =>
      val result = env * 10
      s"result: $result"
    )
  println(customTypeMapZio.unsafeRun(5))

  val repeatMessage
      : CustomTypeMapZio[Int, String] =
    CustomTypeMapZio(env => s"Message \n" * env)
  println(repeatMessage.unsafeRun(5))

case class BigResult(message: String)
@main
def demoTupledEnvironment =
  val squared: CustomTypeMapZio[Int, Unit] =
    CustomTypeMapZio(env =>
      println(
        "Environment integer squared: " +
          env * env
      )
    )

  val repeatMessage
      : CustomTypeMapZio[String, BigResult] =
    CustomTypeMapZio(message =>
      BigResult(s"Environment message: $message")
    )

  val composedRes: CustomTypeMapZio[
    (Int, String),
    BigResult
  ] = squared.flatMap(_ => repeatMessage)

  val finalResult =
    composedRes.unsafeRun((5, "Hello"))
  println(finalResult)
end demoTupledEnvironment

import zio.ZIO
