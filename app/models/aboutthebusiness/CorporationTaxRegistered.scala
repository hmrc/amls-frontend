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
import play.api.libs.json._
import cats.data.Validated.{Invalid, Valid}

sealed trait CorporationTaxRegistered

case class CorporationTaxRegisteredYes(corporationTaxReference : String) extends CorporationTaxRegistered
case object CorporationTaxRegisteredNo extends CorporationTaxRegistered


object CorporationTaxRegistered {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, CorporationTaxRegistered] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "registeredForCorporationTax").read[Boolean].withMessage("error.required.atb.corporation.tax") flatMap {
      case true =>
        (__ \ "corporationTaxReference").read(corporationTaxType) map CorporationTaxRegisteredYes.apply
      case false => Rule.fromMapping { _ => Valid(CorporationTaxRegisteredNo) }
    }
  }

  implicit val formWrites: Write[CorporationTaxRegistered, UrlFormEncoded] = Write {
    case CorporationTaxRegisteredYes(value) =>
      Map("registeredForCorporationTax" -> Seq("true"),
        "corporationTaxReference" -> Seq(value)
      )
    case CorporationTaxRegisteredNo => Map("registeredForCorporationTax" -> Seq("false"))
  }

  implicit val jsonReads: Reads[CorporationTaxRegistered] =
    (__ \ "registeredForCorporationTax").read[Boolean] flatMap {
      case true => (__ \ "corporationTaxReference").read[String] map (CorporationTaxRegisteredYes.apply _)
      case false => Reads(_ => JsSuccess(CorporationTaxRegisteredNo))
    }

  implicit val jsonWrites = Writes[CorporationTaxRegistered] {
    case CorporationTaxRegisteredYes(value) => Json.obj(
      "registeredForCorporationTax" -> true,
      "corporationTaxReference" -> value
    )
    case CorporationTaxRegisteredNo => Json.obj("registeredForCorporationTax" -> false)
  }
}
