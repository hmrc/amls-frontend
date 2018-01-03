/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import jto.validation._
import jto.validation.forms.PM.PM
import jto.validation.forms._
import jto.validation.ValidationError
import play.api.libs.functional.{Functor, Monoid}
import jto.validation.GenericRules
import jto.validation.forms.PM._
import play.api.libs.json.{JsValue, Json, JsObject}

import scala.collection.{GenTraversableOnce, TraversableLike}
import cats.data.Validated.{Valid, Invalid}
object TraversableValidators {

  implicit def seqToOptionSeq[A]
  (implicit
   r: A => Option[A]
  ): Rule[Seq[A], Seq[Option[A]]] =
    Rule.zero[Seq[A]] map {
      _ map r
    }

  implicit def flattenR[A]: Rule[Seq[Option[A]], Seq[A]] =
    Rule.zero[Seq[Option[A]]] map { _ flatten }

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
      case None => Valid(None)
    }
  }
}

object GenericValidators {
  def inList[A](validItems : Traversable[A]) : Rule[A, A] = Rule.fromMapping { item =>
    if (validItems.exists(x => x == item)) {Valid(item)}
    else {Invalid(List(ValidationError("error.not.in.list")))}
  }
}

trait MappingUtils {

  import play.api.libs.json.{Reads, JsSuccess, JsError}

  def constant[A](a: A): Reads[A] = Reads(_ => JsSuccess(a))
  
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

  /**
    * This is an overloaded version of spm from the validation library that will allow
    * duplicates in the Seq that is being written
    *
    * @param w a WriteLike that can write an single item of Type O
    * @tparam O the type of the sequence that this write will be able to handle
    */
  implicit def spm[O](implicit w: WriteLike[O, PM]) =
    Write[Seq[O], PM] { os =>
      os.zipWithIndex
        .map(_.swap)
        .toMap
        .flatMap {
          case (i, o) =>
            repathPM(w.writes(o), (Path \ i) ++ _)
        }
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
      Rule.fromMapping { _ => Valid(b) }

    implicit def toFailureRule[A](f: (Path, Seq[ValidationError])): Rule[UrlFormEncoded, A] =
      Rule { _ => Invalid(f) }

   /*
   * Json reads implicits
   */

    import play.api.libs.json.{Reads, JsSuccess, JsError}

    implicit def toReadsSuccess[A, B <: A](b: B): Reads[A] =
      Reads { _ => JsSuccess(b) }

    implicit def toReadsFailure[A](f: play.api.data.validation.ValidationError): Reads[A] =
      Reads { _ => JsError(f) }

    implicit class RichRule[I, O](rule: Rule[I, O]) {

      def withMessage(message: String*): Rule[I, O] =
        Rule { d =>
          rule.validate(d).leftMap {
            _.map {
              case (p, errs) =>
                p -> (message map { m => ValidationError(m) })
            }
          }
        }

      def validateWith(msg: String = "error.invalid")(fn: O => Boolean): Rule[I, O] =
        rule andThen Rule[O, O] {
          case a if fn(a) =>
            Valid(a)
          case a =>
            Invalid(Seq(Path -> Seq(ValidationError(msg))))
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

  object MonoidImplicits {
    import cats.Monoid
    implicit def jsonMonoid = new Monoid[JsValue] {
      def combine(a1: JsValue, a2: JsValue) = {
        a1.as[JsObject] deepMerge a2.as[JsObject]
      }
      def empty = {
        Json.obj()
      }
    }

    implicit def jsonObjMonoid = new Monoid[JsObject] {
      def combine(a1: JsObject, a2: JsObject) = {
        a1 deepMerge a2
      }
      def empty = {
        Json.obj()
      }
    }

    implicit def urlMonoid = new Monoid[UrlFormEncoded] {
      def combine(a1: UrlFormEncoded, a2: UrlFormEncoded) = a1.++:(a2)
      def empty = Map.empty
    }
  }

  object JsConstraints {

    import play.api.libs.json.Reads

    import play.api.libs.json.Reads._

    def nonEmpty[M](implicit reads: Reads[M], p: M => TraversableLike[_, M]) =
      filter[M](play.api.data.validation.ValidationError("error.required"))(_.isEmpty)
  }


}

object MappingUtils extends MappingUtils
