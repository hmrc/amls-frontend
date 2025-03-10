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

package models.businessactivities

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class TaxMattersSpec extends PlaySpec {

  "Json reads and writes" must {
    "successfully complete a round trip json conversion" in {
      TaxMatters.formats.reads(
        TaxMatters.formats.writes(TaxMatters(false))
      ) must be(JsSuccess(TaxMatters(false), JsPath))
    }

    "Serialise TaxMatters as expected" in {
      Json.toJson(TaxMatters(false)) must be(Json.obj("manageYourTaxAffairs" -> false))
    }

    "Deserialise TaxMatters as expected" in {
      val json = Json.obj("manageYourTaxAffairs" -> false)
      json.as[TaxMatters] must be(TaxMatters(false))
    }
  }
}
