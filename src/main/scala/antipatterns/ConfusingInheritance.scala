package antipatterns

class SomeClass():
  def doSomething() =
    println("someClass.doSomething")

  def doABaseThing() =
    println("doingABaseThing")
    doSomething()

class SomeNewClass() extends SomeClass:

  override def doSomething() =
    super.doSomething()
    println("someNewClass.doSomething")

@main def inheritanceTest() =
  val x = SomeNewClass()
  x.doABaseThing()
