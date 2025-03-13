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

import models.renewal.BusinessTurnover
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ExpectedBusinessTurnoverSpec extends PlaySpec with MockitoSugar {

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "01")) must
        be(JsSuccess(ExpectedBusinessTurnover.First, JsPath))

      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "02")) must
        be(JsSuccess(ExpectedBusinessTurnover.Second, JsPath))

      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "03")) must
        be(JsSuccess(ExpectedBusinessTurnover.Third, JsPath))

      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "04")) must
        be(JsSuccess(ExpectedBusinessTurnover.Fourth, JsPath))

      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "05")) must
        be(JsSuccess(ExpectedBusinessTurnover.Fifth, JsPath))

      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "06")) must
        be(JsSuccess(ExpectedBusinessTurnover.Sixth, JsPath))

      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "07")) must
        be(JsSuccess(ExpectedBusinessTurnover.Seventh, JsPath))
    }

    "write the correct value" in {
      Json.toJson(ExpectedBusinessTurnover.First.asInstanceOf[ExpectedBusinessTurnover]) must
        be(Json.obj("expectedBusinessTurnover" -> "01"))

      Json.toJson(ExpectedBusinessTurnover.Second.asInstanceOf[ExpectedBusinessTurnover]) must
        be(Json.obj("expectedBusinessTurnover" -> "02"))

      Json.toJson(ExpectedBusinessTurnover.Third.asInstanceOf[ExpectedBusinessTurnover]) must
        be(Json.obj("expectedBusinessTurnover" -> "03"))

      Json.toJson(ExpectedBusinessTurnover.Fourth.asInstanceOf[ExpectedBusinessTurnover]) must
        be(Json.obj("expectedBusinessTurnover" -> "04"))

      Json.toJson(ExpectedBusinessTurnover.Fifth.asInstanceOf[ExpectedBusinessTurnover]) must
        be(Json.obj("expectedBusinessTurnover" -> "05"))

      Json.toJson(ExpectedBusinessTurnover.Sixth.asInstanceOf[ExpectedBusinessTurnover]) must
        be(Json.obj("expectedBusinessTurnover" -> "06"))

      Json.toJson(ExpectedBusinessTurnover.Seventh.asInstanceOf[ExpectedBusinessTurnover]) must
        be(Json.obj("expectedBusinessTurnover" -> "07"))
    }

    "throw error for invalid data" in {
      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "20")) must
        be(JsError(JsPath, play.api.libs.json.JsonValidationError("error.invalid")))
    }

    "convert ExpectedBusinessTurnover to renewal BusinessTurnover model" in {
      ExpectedBusinessTurnover.convert(ExpectedBusinessTurnover.First)   must be(BusinessTurnover.First)
      ExpectedBusinessTurnover.convert(ExpectedBusinessTurnover.Second)  must be(BusinessTurnover.Second)
      ExpectedBusinessTurnover.convert(ExpectedBusinessTurnover.Third)   must be(BusinessTurnover.Third)
      ExpectedBusinessTurnover.convert(ExpectedBusinessTurnover.Fourth)  must be(BusinessTurnover.Fourth)
      ExpectedBusinessTurnover.convert(ExpectedBusinessTurnover.Fifth)   must be(BusinessTurnover.Fifth)
      ExpectedBusinessTurnover.convert(ExpectedBusinessTurnover.Sixth)   must be(BusinessTurnover.Sixth)
      ExpectedBusinessTurnover.convert(ExpectedBusinessTurnover.Seventh) must be(BusinessTurnover.Seventh)

    }
  }
}
