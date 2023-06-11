package monads

// TODO Consider deletion, since we are reducing the scope of the book
enum Operation:

  def flatMap(
      f: String => Operation
  ): Operation =
    this match
      case _: Bad =>
        this
      case s: Good =>
        f(s.content)

  def map(f: String => String): Operation =
    this match
      case _: Bad =>
        this
      case Good(content) =>
        Good(f(content))

  case Bad(reason: String)
  case Good(content: String)
end Operation

def httpOperation(
    content: String,
    result: String
): Operation =
  if (content.contains("DbResult=Expected"))
    Operation
      .Good(content + s" + HttpResult=$result")
  else
    Operation
      .Bad("Did not have required data from DB")

def businessLogic(
    content: String,
    result: String
): Operation =
  if (content.contains("HttpResult=Expected"))
    Operation
      .Good(content + s" + LogicResult=$result")
  else
    Operation.Bad(
      "Did not have required data from Http Call"
    )

@main
def happyPath =
  println(
    for
      dbContent <-
        Operation.Good("DbResult=Expected")
      httpContent <-
        httpOperation(dbContent, "Expected")
      logicContent <-
        businessLogic(httpContent, "Expected")
    yield logicContent
  )

@main
def sadPathDb =
  println(
    for
      dbContent <-
        Operation.Good("DbResult=Unexpected")
      httpContent <-
        httpOperation(dbContent, "Expected")
      logicContent <-
        businessLogic(httpContent, "Expected")
    yield logicContent
  )

@main
def sadPathHttp =
  println(
    for
      dbContent <-
        Operation.Good("DbResult=Expected")
      httpContent <-
        httpOperation(dbContent, "Unexpected")
      logicContent <-
        businessLogic(httpContent, "Expected")
    yield logicContent
  )

@main
def failAfterSpecifiedNumber =
  def operationConstructor(
      x: Int,
      limit: Int
  ): Operation =
    if (x < limit)
      Operation.Good(s"Finished step $x")
    else
      Operation.Bad("Failed after max number")

  def badAfterXInvocations(x: Int): Operation =
    for
      result1 <- operationConstructor(0, x)
      result2 <- operationConstructor(1, x)
      result3 <- operationConstructor(2, x)
      result4 <- operationConstructor(3, x)
    yield result4

  println(badAfterXInvocations(5))
end failAfterSpecifiedNumber
