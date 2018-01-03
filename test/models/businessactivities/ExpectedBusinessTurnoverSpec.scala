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

package models.businessactivities

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import models.renewal.BusinessTurnover
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ExpectedBusinessTurnoverSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {
      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("01"))) must
        be(Valid(ExpectedBusinessTurnover.First))

      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("02"))) must
        be(Valid(ExpectedBusinessTurnover.Second))

      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("03"))) must
        be(Valid(ExpectedBusinessTurnover.Third))

      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("04"))) must
        be(Valid(ExpectedBusinessTurnover.Fourth))

      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("05"))) must
        be(Valid(ExpectedBusinessTurnover.Fifth))

      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("06"))) must
        be(Valid(ExpectedBusinessTurnover.Sixth))

      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("07"))) must
        be(Valid(ExpectedBusinessTurnover.Seventh))
    }

    "throw error on missing data" in {
      ExpectedBusinessTurnover.formRule.validate(Map.empty) must
        be(Invalid(Seq((Path \ "expectedBusinessTurnover", Seq(ValidationError("error.required.ba.business.turnover"))))))
    }

    "throw error on invalid data" in {
      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("20"))) must
        be(Invalid(Seq((Path \ "expectedBusinessTurnover", Seq(ValidationError("error.invalid"))))))
    }

    "write correct data from enum value" in {

      ExpectedBusinessTurnover.formWrites.writes(ExpectedBusinessTurnover.First) must
        be(Map("expectedBusinessTurnover" -> Seq("01")))

      ExpectedBusinessTurnover.formWrites.writes(ExpectedBusinessTurnover.Second) must
        be(Map("expectedBusinessTurnover" -> Seq("02")))

      ExpectedBusinessTurnover.formWrites.writes(ExpectedBusinessTurnover.Third) must
        be(Map("expectedBusinessTurnover" -> Seq("03")))

      ExpectedBusinessTurnover.formWrites.writes(ExpectedBusinessTurnover.Fourth) must
        be(Map("expectedBusinessTurnover" -> Seq("04")))

      ExpectedBusinessTurnover.formWrites.writes(ExpectedBusinessTurnover.Fifth) must
        be(Map("expectedBusinessTurnover" -> Seq("05")))

      ExpectedBusinessTurnover.formWrites.writes(ExpectedBusinessTurnover.Sixth) must
        be(Map("expectedBusinessTurnover" -> Seq("06")))

      ExpectedBusinessTurnover.formWrites.writes(ExpectedBusinessTurnover.Seventh) must
        be(Map("expectedBusinessTurnover" -> Seq("07")))
    }
  }

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
      Json.toJson(ExpectedBusinessTurnover.First) must
        be(Json.obj("expectedBusinessTurnover" -> "01"))

      Json.toJson(ExpectedBusinessTurnover.Second) must
        be(Json.obj("expectedBusinessTurnover" -> "02"))

      Json.toJson(ExpectedBusinessTurnover.Third) must
        be(Json.obj("expectedBusinessTurnover" -> "03"))

      Json.toJson(ExpectedBusinessTurnover.Fourth) must
        be(Json.obj("expectedBusinessTurnover" -> "04"))

      Json.toJson(ExpectedBusinessTurnover.Fifth) must
        be(Json.obj("expectedBusinessTurnover" -> "05"))

      Json.toJson(ExpectedBusinessTurnover.Sixth) must
        be(Json.obj("expectedBusinessTurnover" -> "06"))

      Json.toJson(ExpectedBusinessTurnover.Seventh) must
        be(Json.obj("expectedBusinessTurnover" -> "07"))
    }

    "throw error for invalid data" in {
      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "20")) must
        be(JsError(JsPath, play.api.data.validation.ValidationError("error.invalid")))
    }

    "convert ExpectedBusinessTurnover to renewal BusinessTurnover model" in {
      ExpectedBusinessTurnover.convert(ExpectedBusinessTurnover.First) must be(BusinessTurnover.First)
      ExpectedBusinessTurnover.convert(ExpectedBusinessTurnover.Second) must be(BusinessTurnover.Second)
      ExpectedBusinessTurnover.convert(ExpectedBusinessTurnover.Third) must be(BusinessTurnover.Third)
      ExpectedBusinessTurnover.convert(ExpectedBusinessTurnover.Fourth) must be(BusinessTurnover.Fourth)
      ExpectedBusinessTurnover.convert(ExpectedBusinessTurnover.Fifth) must be(BusinessTurnover.Fifth)
      ExpectedBusinessTurnover.convert(ExpectedBusinessTurnover.Sixth) must be(BusinessTurnover.Sixth)
      ExpectedBusinessTurnover.convert(ExpectedBusinessTurnover.Seventh) must be(BusinessTurnover.Seventh)

    }
  }
}
