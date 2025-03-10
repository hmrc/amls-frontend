/*
 * Copyright 2024 HM Revenue & Customs
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

package models.businessdetails

import play.api.libs.json._

sealed trait CorporationTaxRegistered

case class CorporationTaxRegisteredYes(corporationTaxReference: String) extends CorporationTaxRegistered
case object CorporationTaxRegisteredNo extends CorporationTaxRegistered

object CorporationTaxRegistered {

  implicit val jsonReads: Reads[CorporationTaxRegistered] =
    (__ \ "registeredForCorporationTax").read[Boolean] flatMap {
      case true  => (__ \ "corporationTaxReference").read[String] map (CorporationTaxRegisteredYes.apply _)
      case false => Reads(_ => JsSuccess(CorporationTaxRegisteredNo))
    }

  implicit val jsonWrites: Writes[CorporationTaxRegistered] = Writes[CorporationTaxRegistered] {
    case CorporationTaxRegisteredYes(value) =>
      Json.obj(
        "registeredForCorporationTax" -> true,
        "corporationTaxReference"     -> value
      )
    case CorporationTaxRegisteredNo         => Json.obj("registeredForCorporationTax" -> false)
  }
}
