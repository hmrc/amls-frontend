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

class BusinessTurnoverSpec extends PlaySpec with MockitoSugar {

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[BusinessTurnover](Json.obj("businessTurnover" -> "01")) must
        be(JsSuccess(BusinessTurnover.First, JsPath))

      Json.fromJson[BusinessTurnover](Json.obj("businessTurnover" -> "02")) must
        be(JsSuccess(BusinessTurnover.Second, JsPath))

      Json.fromJson[BusinessTurnover](Json.obj("businessTurnover" -> "03")) must
        be(JsSuccess(BusinessTurnover.Third, JsPath))

      Json.fromJson[BusinessTurnover](Json.obj("businessTurnover" -> "04")) must
        be(JsSuccess(BusinessTurnover.Fourth, JsPath))

      Json.fromJson[BusinessTurnover](Json.obj("businessTurnover" -> "05")) must
        be(JsSuccess(BusinessTurnover.Fifth, JsPath))

      Json.fromJson[BusinessTurnover](Json.obj("businessTurnover" -> "06")) must
        be(JsSuccess(BusinessTurnover.Sixth, JsPath))

      Json.fromJson[BusinessTurnover](Json.obj("businessTurnover" -> "07")) must
        be(JsSuccess(BusinessTurnover.Seventh, JsPath))
    }

    "write the correct value" in {
      Json.toJson(BusinessTurnover.First.asInstanceOf[BusinessTurnover]) must
        be(Json.obj("businessTurnover" -> "01"))

      Json.toJson(BusinessTurnover.Second.asInstanceOf[BusinessTurnover]) must
        be(Json.obj("businessTurnover" -> "02"))

      Json.toJson(BusinessTurnover.Third.asInstanceOf[BusinessTurnover]) must
        be(Json.obj("businessTurnover" -> "03"))

      Json.toJson(BusinessTurnover.Fourth.asInstanceOf[BusinessTurnover]) must
        be(Json.obj("businessTurnover" -> "04"))

      Json.toJson(BusinessTurnover.Fifth.asInstanceOf[BusinessTurnover]) must
        be(Json.obj("businessTurnover" -> "05"))

      Json.toJson(BusinessTurnover.Sixth.asInstanceOf[BusinessTurnover]) must
        be(Json.obj("businessTurnover" -> "06"))

      Json.toJson(BusinessTurnover.Seventh.asInstanceOf[BusinessTurnover]) must
        be(Json.obj("businessTurnover" -> "07"))
    }

    "throw error for invalid data" in {
      Json.fromJson[BusinessTurnover](Json.obj("businessTurnover" -> "20")) must
        be(JsError(JsPath, play.api.libs.json.JsonValidationError("error.invalid")))
    }
  }
}
