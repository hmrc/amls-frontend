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

package models.tradingpremises

import org.scalatestplus.play.PlaySpec
import models.tradingpremises.BusinessStructure._
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class BusinessStructureSpec extends PlaySpec {

  "BusinessStructure" should {

    "Read JSON data successfully" in {
      Json.fromJson[BusinessStructure](Json.obj("agentsBusinessStructure" -> "01")) must be(
        JsSuccess(SoleProprietor, JsPath)
      )

      Json.fromJson[BusinessStructure](Json.obj("agentsBusinessStructure" -> "02")) must be(
        JsSuccess(LimitedLiabilityPartnership, JsPath)
      )

      Json.fromJson[BusinessStructure](Json.obj("agentsBusinessStructure" -> "03")) must be(
        JsSuccess(Partnership, JsPath)
      )

      Json.fromJson[BusinessStructure](Json.obj("agentsBusinessStructure" -> "04")) must be(
        JsSuccess(IncorporatedBody, JsPath)
      )

      Json.fromJson[BusinessStructure](Json.obj("agentsBusinessStructure" -> "05")) must be(
        JsSuccess(UnincorporatedBody, JsPath)
      )
    }

    "Write JSON data successfully" in {

      Json.toJson(SoleProprietor.asInstanceOf[BusinessStructure])              must be(Json.obj("agentsBusinessStructure" -> "01"))
      Json.toJson(LimitedLiabilityPartnership.asInstanceOf[BusinessStructure]) must be(
        Json.obj("agentsBusinessStructure" -> "02")
      )
      Json.toJson(Partnership.asInstanceOf[BusinessStructure])                 must be(Json.obj("agentsBusinessStructure" -> "03"))
      Json.toJson(IncorporatedBody.asInstanceOf[BusinessStructure])            must be(Json.obj("agentsBusinessStructure" -> "04"))
      Json.toJson(UnincorporatedBody.asInstanceOf[BusinessStructure])          must be(
        Json.obj("agentsBusinessStructure" -> "05")
      )
    }

    "throw error for invalid data" in {
      Json.fromJson[BusinessStructure](Json.obj("agentsBusinessStructure" -> "20")) must
        be(JsError(JsPath, play.api.libs.json.JsonValidationError("error.invalid")))
    }
  }

}
