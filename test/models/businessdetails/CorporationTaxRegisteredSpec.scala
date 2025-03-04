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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class CorporationTaxRegisteredSpec extends PlaySpec with MockitoSugar {

  "JSON validation" must {

    "successfully validate given an false value" in {
      Json.fromJson[CorporationTaxRegistered](Json.obj("registeredForCorporationTax" -> false)) must
        be(JsSuccess(CorporationTaxRegisteredNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("registeredForCorporationTax" -> true, "corporationTaxReference" -> "1234567890")

      Json.fromJson[CorporationTaxRegistered](json) must
        be(JsSuccess(CorporationTaxRegisteredYes("1234567890"), JsPath \ "corporationTaxReference"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("registeredForCorporationTax" -> true)

      Json.fromJson[CorporationTaxRegistered](json) must
        be(JsError((JsPath \ "corporationTaxReference") -> JsonValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(CorporationTaxRegisteredNo.asInstanceOf[CorporationTaxRegistered]) must
        be(Json.obj("registeredForCorporationTax" -> false))

      Json.toJson(CorporationTaxRegisteredYes("1234567890").asInstanceOf[CorporationTaxRegistered]) must
        be(
          Json.obj(
            "registeredForCorporationTax" -> true,
            "corporationTaxReference"     -> "1234567890"
          )
        )
    }
  }
}
