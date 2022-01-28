package ZIOFromNothing



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
    def unsafeRun(env: ENV): RESULT =
        run(env)

@main
def demo =
    val customTypeMapZio: CustomTypeMapZio[Int, String] = 
        CustomTypeMapZio(
                env =>
                    val result = env * 10
                    s"result: $result"
        )
    println(customTypeMapZio.unsafeRun(5))


    val repeatMessage: CustomTypeMapZio[Int, String] = 
        CustomTypeMapZio(
            env => s"Message \n" * env
        )
    println(repeatMessage.unsafeRun(5))
