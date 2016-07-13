package utils

import play.api.data.mapping._
import play.api.data.mapping.forms._
import play.api.data.validation.ValidationError
import play.api.libs.functional.{Functor, Monoid}
import play.api.data.mapping.GenericRules
import play.api.data.mapping.forms.PM.PM

import scala.collection.{GenTraversableOnce, TraversableLike}

object TraversableValidators {

  implicit def seqToOptionSeq[A]
  (implicit
   r: A => Option[A]
  ): Rule[Seq[A], Seq[Option[A]]] =
    Rule.zero[Seq[A]] fmap {
      _ map r
    }

  implicit def flattenR[A]: Rule[Seq[Option[A]], Seq[A]] =
    Rule.zero[Seq[Option[A]]] fmap { _ flatten }

  def minLengthR[T <: Traversable[_]](l : Int) : Rule[T, T] =
    GenericRules.validateWith[T]("error.required") {
      _.size >= l
    }
    
  def maxLengthR[T <: Traversable[_]](l: Int): Rule[T, T] =
    GenericRules.validateWith[T]("error.maxLength", l) {
      _.size <= l
    }
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

  /**
    * This is an overloaded version of `writeM` from the validation library which instead of serializing
    * arrays to:
    *   `a[0] -> "1", a[1] -> "2"`
    * it will serialize to:
    *   `a[] -> "1", "2"`
    */
  implicit def writeM[I](path: Path)(implicit w: WriteLike[I, PM]) = Write[I, UrlFormEncoded] { i =>

    def toM(pm: PM): UrlFormEncoded = {
      pm.foldLeft[Map[Path, Seq[(Path, String)]]](Map.empty) {
        case (m, (p, v)) =>
          val path = Path(p.path.foldLeft[List[PathNode]](List.empty) {
            case (list, n: IdxPathNode) =>
              val last = list.last match {
                case KeyPathNode(x) =>
                  KeyPathNode(s"$x[]")
                case n => n
              }
              list.init :+ last
            case (list, n) =>
              list :+ n
          })
          m.updated(path, m.getOrElse(path, Seq.empty) :+ (path, v))
      }.map {
        case (p, vs) =>
          {PM.asKey(p)} -> vs.map(_._2)
      }
    }

    toM(PM.repathPM(w.writes(i), path ++ _))
  }

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

      def validateWith(msg: String = "error.invalid")(fn: O => Boolean): Rule[I, O] =
        rule compose Rule[O, O] {
          case a if fn(a) =>
            Success(a)
          case a =>
            Failure(Seq(Path -> Seq(ValidationError(msg))))
        }
    }

    implicit class RichWrite[I, O](w1: Write[I, O]) {

      def andThen[I2](w2: Write[I2, I]): Write[I2, O] =
        Write {
          i =>
            w1.writes(w2.writes(i))
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
