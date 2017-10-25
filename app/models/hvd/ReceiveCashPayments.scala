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

package models.hvd

import jto.validation._
import jto.validation.forms._
import models.renewal.{PaymentMethods => RPaymentMethods, ReceiveCashPayments => RReceiveCashPayments}
import play.api.libs.json.{Writes, _}

case class ReceiveCashPayments(receiving: Option[Boolean], paymentMethods: Option[PaymentMethods])

sealed trait ReceiveCashPayments0 {


  implicit val formRule: Rule[UrlFormEncoded, ReceiveCashPayments] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "paymentMethods").read[PaymentMethods] map (x => ReceiveCashPayments(Some(true), Some(x)))
  }

  implicit val jsonReads: Reads[ReceiveCashPayments] =
    (__ \ "receivePayments").read[Boolean] flatMap {
      case true => (__ \ "paymentMethods").read[PaymentMethods] map (x => ReceiveCashPayments(Some(true), Some(x)))
      case false => Reads(_ => JsSuccess(ReceiveCashPayments(Some(false), None)))
    }

  private implicit def write[A]
  (implicit
   mon:cats.Monoid[A],
   a: Path => WriteLike[Boolean, A],
   b: Path => WriteLike[Option[PaymentMethods] , A]
  ): Write[ReceiveCashPayments, A] =
    To[A] { __ =>
      (
        (__ \ "receivePayments").write[Boolean].contramap[Option[_]] {
          case Some(_) => true
          case None => false
        } ~
          (__ \ "paymentMethods").write[Option[PaymentMethods]]
        )(a => (a.paymentMethods, a.paymentMethods))
    }

  val formR: Rule[UrlFormEncoded, ReceiveCashPayments] = {
    implicitly[Rule[UrlFormEncoded, ReceiveCashPayments]]
  }

 val formW: Write[ReceiveCashPayments, UrlFormEncoded] = {
    implicitly
  }
  val jsonR: Reads[ReceiveCashPayments] = {
    implicitly[Reads[ReceiveCashPayments]]
  }
  val jsonW = Writes[ReceiveCashPayments] {x =>
   x.paymentMethods match {
     case Some(paymentMtds) => Json.obj("receivePayments" -> true,
       "paymentMethods" -> Json.obj("courier" -> paymentMtds.courier,
       "direct" -> paymentMtds.direct,
       "other" -> paymentMtds.other.isDefined,
       "details" -> paymentMtds.other
     ))
     case None =>  Json.obj("receivePayments" -> false, "paymentMethods" -> Json.obj())
   }
  }
}

object ReceiveCashPayments {

  private object Cache extends ReceiveCashPayments0

  implicit val formR: Rule[UrlFormEncoded, ReceiveCashPayments] = Cache.formR
  implicit val jsonR: Reads[ReceiveCashPayments] = Cache.jsonR
  implicit val formW: Write[ReceiveCashPayments, UrlFormEncoded] = Cache.formW
  implicit val jsonW: Writes[ReceiveCashPayments] = Cache.jsonW


  def convert(model: ReceiveCashPayments) : RReceiveCashPayments = {
    model.paymentMethods match {
      case Some(paymentMtd) => RReceiveCashPayments(Some(RPaymentMethods(paymentMtd.courier, paymentMtd.direct, paymentMtd.other)))
      case _ => RReceiveCashPayments(None)
    }
  }
}
