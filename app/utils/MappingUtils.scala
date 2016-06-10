package utils

import play.api.data.mapping._
import play.api.data.mapping.forms._
import play.api.data.validation.ValidationError
import play.api.libs.functional.{Functor, Monoid}
import play.api.data.mapping.GenericRules
import scala.collection.TraversableLike

object TraversableValidators {
  def minLength[T <: Traversable[_]](expectedLength : Int) : Rule[T, T] = {
    GenericRules.validateWith[T]("error.required")(t => t.size >= expectedLength)}

  def maxLength[T <: Traversable[_]](expectedLength : Int) : Rule[T, T] = {
    GenericRules.validateWith[T]("error.tooLarge")(t => t.size <= expectedLength)}
}

object OptionValidators {
  def ifPresent[A](inner: Rule[A, A]): RuleLike[Option[A], Option[A]] = {
    Rule[Option[A], Option[A]] {
      case Some(a) => inner.validate(a).map(x => Some(x))
      case None => Success(None)
    }
  }
}

trait MappingUtils {

  object Implicits {
    /*
   * Basic wrapping conversions to make writing mappings easier
   * Would be nice to be able to write these in a more `functional`
   * manner
   */
    implicit def toSeq[A](a: A): Seq[A] = Seq(a)

    implicit def toMap[A, B](t: (A, B)): Map[A, B] = Map(t)

    implicit def toMap2[A, B](t: (A, B)): Map[A, Seq[B]] = Map(t._1 -> Seq(t._2))

    /*
   * Form rule implicits
   */
    implicit def toSuccessRule[A, B <: A](b: B): Rule[UrlFormEncoded, A] =
      Rule.fromMapping { _ => Success(b) }

    implicit def toFailureRule[A](f: (Path, Seq[ValidationError])): Rule[UrlFormEncoded, A] =
      Rule { _ => Failure(f) }


    /*
   * Json reads implicits
   */

    import play.api.libs.json.{Reads, JsSuccess, JsError}

    implicit def toReadsSuccess[A, B <: A](b: B): Reads[A] =
      Reads { _ => JsSuccess(b) }

    implicit def toReadsFailure[A](f: ValidationError): Reads[A] =
      Reads { _ => JsError(f) }

    implicit class RichRule[I, O](rule: Rule[I, O]) {
      def withMessage(message: String*): Rule[I, O] =
        Rule { d =>
          rule.validate(d).fail.map {
            _.map {
              case (p, errs) =>
                p -> (message map { m => ValidationError(m) })
            }
          }
        }
    }
  }

  object JsConstraints {

    import play.api.libs.json.Reads

    import play.api.libs.json.Reads._

    def nonEmpty[M](implicit reads: Reads[M], p: M => TraversableLike[_, M]) =
      filter[M](ValidationError("error.required"))(_.isEmpty)
  }


}

object MappingUtils extends MappingUtils
