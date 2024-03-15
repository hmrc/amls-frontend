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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import models.renewal.AMLSTurnover
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ExpectedAMLSTurnoverSpec extends PlaySpec with MockitoSugar {

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "01")) must
        be(JsSuccess(ExpectedAMLSTurnover.First, JsPath))

      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "02")) must
        be(JsSuccess(ExpectedAMLSTurnover.Second, JsPath))

      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "03")) must
        be(JsSuccess(ExpectedAMLSTurnover.Third, JsPath))

      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "04")) must
        be(JsSuccess(ExpectedAMLSTurnover.Fourth, JsPath))

      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "05")) must
        be(JsSuccess(ExpectedAMLSTurnover.Fifth, JsPath))

      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "06")) must
        be(JsSuccess(ExpectedAMLSTurnover.Sixth, JsPath))

      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "07")) must
        be(JsSuccess(ExpectedAMLSTurnover.Seventh, JsPath))
    }

    "write the correct value" in {

      Json.toJson(ExpectedAMLSTurnover.First.asInstanceOf[ExpectedAMLSTurnover]) must
        be(Json.obj("expectedAMLSTurnover" -> "01"))

      Json.toJson(ExpectedAMLSTurnover.Second.asInstanceOf[ExpectedAMLSTurnover]) must
        be(Json.obj("expectedAMLSTurnover" -> "02"))

      Json.toJson(ExpectedAMLSTurnover.Third.asInstanceOf[ExpectedAMLSTurnover]) must
        be(Json.obj("expectedAMLSTurnover" -> "03"))

      Json.toJson(ExpectedAMLSTurnover.Fourth.asInstanceOf[ExpectedAMLSTurnover]) must
        be(Json.obj("expectedAMLSTurnover" -> "04"))

      Json.toJson(ExpectedAMLSTurnover.Fifth.asInstanceOf[ExpectedAMLSTurnover]) must
        be(Json.obj("expectedAMLSTurnover" -> "05"))

      Json.toJson(ExpectedAMLSTurnover.Sixth.asInstanceOf[ExpectedAMLSTurnover]) must
        be(Json.obj("expectedAMLSTurnover" -> "06"))

      Json.toJson(ExpectedAMLSTurnover.Seventh.asInstanceOf[ExpectedAMLSTurnover]) must
        be(Json.obj("expectedAMLSTurnover" -> "07"))
    }

    "throw error for invalid data" in {
      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "20")) must
        be(JsError(JsPath, play.api.libs.json.JsonValidationError("error.invalid")))
    }

    "convert ExpectedAMLSTurnover to renewal AMLSTurnover model" in {
      ExpectedAMLSTurnover.convert(ExpectedAMLSTurnover.First) mustBe AMLSTurnover.First
      ExpectedAMLSTurnover.convert(ExpectedAMLSTurnover.Second) mustBe AMLSTurnover.Second
      ExpectedAMLSTurnover.convert(ExpectedAMLSTurnover.Third) mustBe AMLSTurnover.Third
      ExpectedAMLSTurnover.convert(ExpectedAMLSTurnover.Fourth) mustBe AMLSTurnover.Fourth
      ExpectedAMLSTurnover.convert(ExpectedAMLSTurnover.Fifth) mustBe AMLSTurnover.Fifth
      ExpectedAMLSTurnover.convert(ExpectedAMLSTurnover.Sixth) mustBe AMLSTurnover.Sixth
      ExpectedAMLSTurnover.convert(ExpectedAMLSTurnover.Seventh) mustBe AMLSTurnover.Seventh

    }
  }
}
