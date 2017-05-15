/*
 * Copyright 2017 HM Revenue & Customs
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

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class BusinessTurnoverSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {
      BusinessTurnover.formRule.validate(Map("businessTurnover" -> Seq("01"))) must
        be(Valid(BusinessTurnover.First))

      BusinessTurnover.formRule.validate(Map("businessTurnover" -> Seq("02"))) must
        be(Valid(BusinessTurnover.Second))

      BusinessTurnover.formRule.validate(Map("businessTurnover" -> Seq("03"))) must
        be(Valid(BusinessTurnover.Third))

      BusinessTurnover.formRule.validate(Map("businessTurnover" -> Seq("04"))) must
        be(Valid(BusinessTurnover.Fourth))

      BusinessTurnover.formRule.validate(Map("businessTurnover" -> Seq("05"))) must
        be(Valid(BusinessTurnover.Fifth))

      BusinessTurnover.formRule.validate(Map("businessTurnover" -> Seq("06"))) must
        be(Valid(BusinessTurnover.Sixth))

      BusinessTurnover.formRule.validate(Map("businessTurnover" -> Seq("07"))) must
        be(Valid(BusinessTurnover.Seventh))
    }

    "throw error on missing data" in {
      BusinessTurnover.formRule.validate(Map.empty) must
        be(Invalid(Seq((Path \ "businessTurnover", Seq(ValidationError("error.required.renewal.ba.business.turnover"))))))
    }

    "throw error on invalid data" in {
      BusinessTurnover.formRule.validate(Map("businessTurnover" -> Seq("20"))) must
        be(Invalid(Seq((Path \ "businessTurnover", Seq(ValidationError("error.invalid"))))))
    }

    "write correct data from enum value" in {

      BusinessTurnover.formWrites.writes(BusinessTurnover.First) must
        be(Map("businessTurnover" -> Seq("01")))

      BusinessTurnover.formWrites.writes(BusinessTurnover.Second) must
        be(Map("businessTurnover" -> Seq("02")))

      BusinessTurnover.formWrites.writes(BusinessTurnover.Third) must
        be(Map("businessTurnover" -> Seq("03")))

      BusinessTurnover.formWrites.writes(BusinessTurnover.Fourth) must
        be(Map("businessTurnover" -> Seq("04")))

      BusinessTurnover.formWrites.writes(BusinessTurnover.Fifth) must
        be(Map("businessTurnover" -> Seq("05")))

      BusinessTurnover.formWrites.writes(BusinessTurnover.Sixth) must
        be(Map("businessTurnover" -> Seq("06")))

      BusinessTurnover.formWrites.writes(BusinessTurnover.Seventh) must
        be(Map("businessTurnover" -> Seq("07")))
    }
  }

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
      Json.toJson(BusinessTurnover.First) must
        be(Json.obj("businessTurnover" -> "01"))

      Json.toJson(BusinessTurnover.Second) must
        be(Json.obj("businessTurnover" -> "02"))

      Json.toJson(BusinessTurnover.Third) must
        be(Json.obj("businessTurnover" -> "03"))

      Json.toJson(BusinessTurnover.Fourth) must
        be(Json.obj("businessTurnover" -> "04"))

      Json.toJson(BusinessTurnover.Fifth) must
        be(Json.obj("businessTurnover" -> "05"))

      Json.toJson(BusinessTurnover.Sixth) must
        be(Json.obj("businessTurnover" -> "06"))

      Json.toJson(BusinessTurnover.Seventh) must
        be(Json.obj("businessTurnover" -> "07"))
    }

    "throw error for invalid data" in {
      Json.fromJson[BusinessTurnover](Json.obj("businessTurnover" -> "20")) must
        be(JsError(JsPath, play.api.data.validation.ValidationError("error.invalid")))
    }
  }
}
