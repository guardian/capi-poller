package data

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, \/, \/-}

object FutureEither {
  def apply[L, R](future: Future[L \/ R]) = new FutureEither(future)
  def apply[L, R](either: L \/ R) = new FutureEither(Future.successful(either))

  type \?/[+L, +R] = FutureEither[L, R]

  def sequence[L, R](ls: Iterable[FutureEither[L, R]])(implicit ec: ExecutionContext): FutureEither[Iterable[L \/ R], Iterable[R]] = FutureEither(
    Future.sequence(ls.map(_.future)) map { ls =>
      if (ls.exists(_.isLeft))
        \/.left(ls)
      else
        \/.right(ls.flatMap(_.toOption))
    }
  )
}

class FutureEither[+L, +R](val future: Future[L \/ R]) {

  def map[RR](fn: R => RR)(implicit ec: ExecutionContext) = FutureEither(future map (_ map fn))

  def flatMap[LL >: L, RR](fn: R => FutureEither[LL, RR])(implicit ec: ExecutionContext): FutureEither[LL, RR] = FutureEither(
    future flatMap {
      case \/-(r) => fn(r).future
      case left @ -\/(_) => Future successful left
    }
  )

  def fold[X](fl: L => X, fr: R => X)(implicit ec: ExecutionContext): Future[X] = future map { _.fold(fl, fr) }

  def leftMap[LL](fn: L => LL)(implicit ec: ExecutionContext) = FutureEither(future map (_ leftMap fn))

  def union[X >: R](implicit leftEv: L <:< X, ec: ExecutionContext): Future[X] = valueOr[X](identity[L])

  def valueOr[RR >: R](x: L => RR)(implicit ec: ExecutionContext): Future[RR] = future map (_ valueOr x)
}