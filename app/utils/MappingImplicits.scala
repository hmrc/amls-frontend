package utils

import play.api.data.mapping.{Failure, Path, Success, Rule}
import play.api.data.mapping.forms._
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsSuccess, Reads}

trait MappingImplicits {

  /*
   * Basic wrapping conversions to make writing mappings easier
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
  implicit def toReadsSuccess[A, B <: A](b: B): Reads[A] =
    Reads { _ => JsSuccess(b) }

  implicit def toReadsFailure[A](f: ValidationError): Reads[A] =
    Reads { _ => JsError(f)}

}

object MappingImplicits extends MappingImplicits
