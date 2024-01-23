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

package models.renewal

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class AMLSTurnoverSpec extends PlaySpec with MockitoSugar {

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "01")) must
        be(JsSuccess(AMLSTurnover.First, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "02")) must
        be(JsSuccess(AMLSTurnover.Second, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "03")) must
        be(JsSuccess(AMLSTurnover.Third, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "04")) must
        be(JsSuccess(AMLSTurnover.Fourth, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "05")) must
        be(JsSuccess(AMLSTurnover.Fifth, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "06")) must
        be(JsSuccess(AMLSTurnover.Sixth, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "07")) must
        be(JsSuccess(AMLSTurnover.Seventh, JsPath))
    }

    "write the correct value" in {

      Json.toJson(AMLSTurnover.First.asInstanceOf[AMLSTurnover]) must
        be(Json.obj("turnover" -> "01"))

      Json.toJson(AMLSTurnover.Second.asInstanceOf[AMLSTurnover]) must
        be(Json.obj("turnover" -> "02"))

      Json.toJson(AMLSTurnover.Third.asInstanceOf[AMLSTurnover]) must
        be(Json.obj("turnover" -> "03"))

      Json.toJson(AMLSTurnover.Fourth.asInstanceOf[AMLSTurnover]) must
        be(Json.obj("turnover" -> "04"))

      Json.toJson(AMLSTurnover.Fifth.asInstanceOf[AMLSTurnover]) must
        be(Json.obj("turnover" -> "05"))

      Json.toJson(AMLSTurnover.Sixth.asInstanceOf[AMLSTurnover]) must
        be(Json.obj("turnover" -> "06"))

      Json.toJson(AMLSTurnover.Seventh.asInstanceOf[AMLSTurnover]) must
        be(Json.obj("turnover" -> "07"))
    }

    "throw error for invalid data" in {
      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "20")) must
        be(JsError(JsPath, play.api.libs.json.JsonValidationError("error.invalid")))
    }
  }
}
