package monads.effects

case object Box
case object Dead
case object Alive

/* def observe(box: Box.type): IO[Dead.type,
 * Alive.type] =
 * Either.cond(Random.nextBoolean(), Alive, Dead)
 *
 * @main def checkKatHealth =
 * val kat = observe(Box) println(kat)
 *
 * case object Angry
 *
 * def kick(kat: Alive.type): Either[Dead.type,
 * Angry.type] =
 * Either.cond(Random.nextBoolean(), Angry, Dead)
 *
 * @main def kickKat =
 * //val kat = kick(observe(Box)) // this doesn't
 * work because you shouldn't kick a dead kat val
 * kat = observe(Box).flatMap(kick) println(kat) */
