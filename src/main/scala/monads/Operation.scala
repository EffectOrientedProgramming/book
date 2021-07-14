package monads

enum Operation:

  def flatMap(f: String => Operation): Operation =
    this match
      case _: Bad  => this
      case s: Good => f(s.content)

  def map(f: String => String): Operation =
    this match
      case _: Bad        => this
      case Good(content) => Good(f(content))

  case Bad(reason: String)
  case Good(content: String)

def httpOperation(content: String, result: String): Operation =
  if (content.contains("DbResult=Expected"))
    Operation.Good(content + s" + HttpResult=$result")
  else
    Operation.Bad("Did not have required data from DB")

def businessLogic(content: String, result: String): Operation =
  if (content.contains("HttpResult=Expected"))
    Operation.Good(content + s" + LogicResult=$result")
  else
    Operation.Bad("Did not have required data from Http Call")

@main def happyPath =
  println(
    for
      dbContent <- Operation.Good("DbResult=Expected")
      httpContent <- httpOperation(dbContent, "Expected")
      logicContent <- businessLogic(httpContent, "Expected")
    yield logicContent
  )

@main def sadPathDb =
  println(
    for
      dbContent <- Operation.Good("DbResult=Unexpected")
      httpContent <- httpOperation(dbContent, "Expected")
      logicContent <- businessLogic(httpContent, "Expected")
    yield logicContent
  )

@main def sadPathHttp =
  println(
    for
      dbContent <- Operation.Good("DbResult=Expected")
      httpContent <- httpOperation(dbContent, "Unexpected")
      logicContent <- businessLogic(httpContent, "Expected")
    yield logicContent
  )
