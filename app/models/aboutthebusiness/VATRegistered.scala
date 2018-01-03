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

package models.aboutthebusiness

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.libs.json._
import cats.data.Validated.{Invalid, Valid}

sealed trait VATRegistered

case class VATRegisteredYes(value : String) extends VATRegistered
case object VATRegisteredNo extends VATRegistered


object VATRegistered {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, VATRegistered] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "registeredForVAT").read[Boolean].withMessage("error.required.atb.registered.for.vat") flatMap {
      case true =>
        (__ \ "vrnNumber").read(vrnType) map VATRegisteredYes.apply
      case false => Rule.fromMapping { _ => Valid(VATRegisteredNo) }
    }
  }

  implicit val formWrites: Write[VATRegistered, UrlFormEncoded] = Write {
    case VATRegisteredYes(value) =>
      Map("registeredForVAT" -> Seq("true"),
        "vrnNumber" -> Seq(value)
      )
    case VATRegisteredNo => Map("registeredForVAT" -> Seq("false"))
  }

  implicit val jsonReads: Reads[VATRegistered] =
    (__ \ "registeredForVAT").read[Boolean] flatMap {
    case true => (__ \ "vrnNumber").read[String] map (VATRegisteredYes.apply _)
    case false => Reads(_ => JsSuccess(VATRegisteredNo))
  }

  implicit val jsonWrites = Writes[VATRegistered] {
    case VATRegisteredYes(value) => Json.obj(
      "registeredForVAT" -> true,
      "vrnNumber" -> value
    )
    case VATRegisteredNo => Json.obj("registeredForVAT" -> false)
  }
}
