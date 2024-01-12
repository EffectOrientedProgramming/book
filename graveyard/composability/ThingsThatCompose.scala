package composability

import zio.*


object ThingsThatCompose extends ZIOAppDefault:
  override def run =
    def saveThatNeverFails(i: Int): Unit =
      ()

    def saveThatFails(i: Int): Unit =
      if (i > 1) throw Exception() else saveThatNeverFails(i)

    def saveTwoThings() =
      saveThatFails(1)
      saveThatFails(2)

    // we can't call saveTwoThings safely (ie it throws but we don't know that)
    // saveTwoThings()

    try
      saveTwoThings()
    catch
      case _ => println("we failed to save something")

    // this conveys the effect can fail with a throwable
    def saveEffect(i: Int): ZIO[Any, Throwable, Unit] =
      ZIO.attempt(saveThatFails(i))

    def explicitlyFailableWorkflow(): ZIO[Any, Throwable, Unit] =
      defer:
        saveEffect(1).run
        saveEffect(2).run

    def composedLogic(): ZIO[Any, Nothing, Unit] =
      explicitlyFailableWorkflow()
        .catchAll( _ => ZIO.debug("Our program failed."))

    composedLogic()

    // possible other example NumberFormatter that isn't thread safe and the only way we know that
    // is by reading the docs

    /*
    NumberFormatter
     */

    //


