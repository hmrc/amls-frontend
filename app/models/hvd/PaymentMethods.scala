/*
 * Copyright 2020 HM Revenue & Customs
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

package models.hvd

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{ValidationError, _}
import jto.validation.forms._
import play.api.libs.json.{Json, Reads, Writes}


case class PaymentMethods(
                         courier: Boolean,
                         direct: Boolean,
                         other: Option[String]
                         )

sealed trait PaymentMethods0 {

  import models.FormTypes._

  private implicit def rule[A]
  (implicit
   s: Path => Rule[A, String],
   b: Path => Rule[A, Option[Boolean]]
  ): Rule[A, PaymentMethods] =
    From[A] { __ =>

      import utils.MappingUtils.Implicits.RichRule

      val minLength = 1
      val maxLength = 255

      def minLengthR(l: Int) = Rule.zero[String].flatMap[String] {
        case s if s.length >= l =>
          Rule(_ => Valid(s))
        case _ =>
          Rule(_ => Invalid(Seq(Path -> Seq(ValidationError("error.minLength", l)))))
      }

      def maxLengthR(l: Int) = Rule.zero[String].flatMap[String] {
        case s if s.length <= l =>
          Rule(_ => Valid(s))
        case _ =>
          Rule(_ => Invalid(Seq(Path -> Seq(ValidationError("error.maxLength", l)))))
      }

      val detailsR: Rule[String, String] =
        (minLengthR(minLength) withMessage "error.required.hvd.describe") andThen
        (maxLengthR(maxLength) withMessage "error.maxlength.hvd.describe") andThen
        basicPunctuationPattern("error.required.hvd.format")

      val booleanR = b andThen { _ map { case Some(b) => b; case None => false } }

      (
        (__ \ "courier").read(booleanR) ~
        (__ \ "direct").read(booleanR) ~
        (__ \ "other").read(booleanR).flatMap[Option[String]] {
          case true =>
            (__ \ "details").read(detailsR) map Some.apply
          case false =>
            Rule(_ => Valid(None))
        }
      )(PaymentMethods.apply).validateWith("error.required.hvd.choose.option"){
        methods =>
          methods.courier || methods.direct || methods.other.isDefined
      }
    }

  private implicit def write: Write[PaymentMethods, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    (
        (__ \ "courier").write[Boolean] ~
        (__ \ "direct").write[Boolean] ~
        (__ \ "other").write[Boolean].contramap[Option[_]] {
          case Some(_) => true
          case None => false
        } ~
        (__ \ "details").write[Option[String]]
      )(a => (a.courier, a.direct, a.other, a.other))
    }

  val formR: Rule[UrlFormEncoded, PaymentMethods] = {
    import jto.validation.forms.Rules._
    implicitly[Rule[UrlFormEncoded, PaymentMethods]]
  }

  val jsonR: Reads[PaymentMethods] = {
    import jto.validation.playjson.Rules.{pickInJson => _, _}
    import utils.JsonMapping._
    implicitly
  }

  val formW: Write[PaymentMethods, UrlFormEncoded] = {
    implicitly[Write[PaymentMethods, UrlFormEncoded]]
  }

  val jsonW = Writes[PaymentMethods] {x =>
    val jsMethods = Json.obj("courier" -> x.courier,
      "direct" -> x.direct,
      "other" -> x.other.isDefined)
    val jsDetails = Json.obj("details" -> x.other)
    x.other.isDefined match {
      case true => jsMethods ++ jsDetails
      case false => jsMethods
    }

  }
}

object PaymentMethods {

  object Cache extends PaymentMethods0

  implicit val formR: Rule[UrlFormEncoded, PaymentMethods] = Cache.formR
  implicit val jsonR: Reads[PaymentMethods] = Cache.jsonR
  implicit val formW: Write[PaymentMethods, UrlFormEncoded] = Cache.formW
  implicit val jsonW: Writes[PaymentMethods] = Cache.jsonW
}
