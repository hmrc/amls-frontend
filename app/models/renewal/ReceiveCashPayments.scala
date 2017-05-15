/*
 * Copyright 2017 HM Revenue & Customs
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

package models.renewal

import jto.validation._
import jto.validation.forms._
import play.api.libs.json.{Writes, _}
import cats.data.Validated.{Invalid, Valid}

case class ReceiveCashPayments(paymentMethods: Option[PaymentMethods])

sealed trait ReceiveCashPayments0 {


  implicit val formRule: Rule[UrlFormEncoded, ReceiveCashPayments] = From[UrlFormEncoded] { __ =>
    import utils.MappingUtils.Implicits.RichRule

    import jto.validation.forms.Rules._

    (__ \ "receivePayments").read[Boolean].withMessage("error.required.renewal.hvd.receive.cash.payments") flatMap {
      case true =>
        (__ \ "paymentMethods").read[PaymentMethods] map (x => ReceiveCashPayments(Some(x)))
      case false => Rule.fromMapping { _ => Valid(ReceiveCashPayments(None)) }
    }
  }

  implicit val jsonReads: Reads[ReceiveCashPayments] =
    (__ \ "receivePayments").read[Boolean] flatMap {
      case true => (__ \ "paymentMethods").read[PaymentMethods] map (x => ReceiveCashPayments(Some(x)))
      case false => Reads(_ => JsSuccess(ReceiveCashPayments(None)))
    }

  private implicit def write[A]
  (implicit
   mon: cats.Monoid[A],
   a: Path => WriteLike[Boolean, A],
   b: Path => WriteLike[Option[PaymentMethods], A]
  ): Write[ReceiveCashPayments, A] =
    To[A] { __ =>
      (
        (__ \ "receivePayments").write[Boolean].contramap[Option[_]] {
          case Some(_) => true
          case None => false
        } ~
          (__ \ "paymentMethods").write[Option[PaymentMethods]]
        ) (a => (a.paymentMethods, a.paymentMethods))
    }

  val formR: Rule[UrlFormEncoded, ReceiveCashPayments] = {
    import jto.validation.forms.Rules._
    implicitly[Rule[UrlFormEncoded, ReceiveCashPayments]]
  }

  val formW: Write[ReceiveCashPayments, UrlFormEncoded] = {
    import cats.implicits._
    import utils.MappingUtils.MonoidImplicits.urlMonoid
    import jto.validation.forms.Writes._
    implicitly
  }
  val jsonR: Reads[ReceiveCashPayments] = {
    implicitly[Reads[ReceiveCashPayments]]
  }
  val jsonW = Writes[ReceiveCashPayments] { x =>
    x.paymentMethods match {
      case Some(paymentMtds) => Json.obj("receivePayments" -> true,
        "paymentMethods" -> Json.obj(
          "courier" -> paymentMtds.courier,
          "direct" -> paymentMtds.direct,
          "other" -> paymentMtds.other.isDefined,
          "details" -> paymentMtds.other
        ))
      case None => Json.obj("receivePayments" -> false, "paymentMethods" -> Json.obj())
    }
  }

}

object ReceiveCashPayments {

  private object Cache extends ReceiveCashPayments0

  implicit val formR: Rule[UrlFormEncoded, ReceiveCashPayments] = Cache.formR
  implicit val jsonR: Reads[ReceiveCashPayments] = Cache.jsonR
  implicit val formW: Write[ReceiveCashPayments, UrlFormEncoded] = Cache.formW
  implicit val jsonW: Writes[ReceiveCashPayments] = Cache.jsonW

  implicit def convert(model: ReceiveCashPayments): models.hvd.ReceiveCashPayments = model.paymentMethods match {
    case Some(paymentMethods) =>
      models.hvd.ReceiveCashPayments(Some(models.hvd.PaymentMethods(paymentMethods.courier, paymentMethods.direct, paymentMethods.other)))
    case None => models.hvd.ReceiveCashPayments(None)
  }
}
