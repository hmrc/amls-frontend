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

package models.businessactivities

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ExpectedAMLSTurnoverSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {

      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("01"))) must
        be(Valid(ExpectedAMLSTurnover.First))

      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("02"))) must
        be(Valid(ExpectedAMLSTurnover.Second))

      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("03"))) must
        be(Valid(ExpectedAMLSTurnover.Third))

      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("04"))) must
        be(Valid(ExpectedAMLSTurnover.Fourth))

      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("05"))) must
        be(Valid(ExpectedAMLSTurnover.Fifth))

      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("06"))) must
        be(Valid(ExpectedAMLSTurnover.Sixth))

      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("07"))) must
        be(Valid(ExpectedAMLSTurnover.Seventh))
    }

    "write correct data from enum value" in {

      ExpectedAMLSTurnover.formWrites.writes(ExpectedAMLSTurnover.First) must
        be(Map("expectedAMLSTurnover" -> Seq("01")))

      ExpectedAMLSTurnover.formWrites.writes(ExpectedAMLSTurnover.Second) must
        be(Map("expectedAMLSTurnover" -> Seq("02")))

      ExpectedAMLSTurnover.formWrites.writes(ExpectedAMLSTurnover.Third) must
        be(Map("expectedAMLSTurnover" -> Seq("03")))

      ExpectedAMLSTurnover.formWrites.writes(ExpectedAMLSTurnover.Fourth) must
        be(Map("expectedAMLSTurnover" -> Seq("04")))

      ExpectedAMLSTurnover.formWrites.writes(ExpectedAMLSTurnover.Fifth) must
        be(Map("expectedAMLSTurnover" -> Seq("05")))

      ExpectedAMLSTurnover.formWrites.writes(ExpectedAMLSTurnover.Sixth) must
        be(Map("expectedAMLSTurnover" -> Seq("06")))

      ExpectedAMLSTurnover.formWrites.writes(ExpectedAMLSTurnover.Seventh) must
        be(Map("expectedAMLSTurnover" -> Seq("07")))
    }


    "throw error on invalid data" in {
      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("20"))) must
        be(Invalid(Seq((Path \ "expectedAMLSTurnover", Seq(ValidationError("error.invalid"))))))
    }

    "throw error on empty data" in {
      ExpectedAMLSTurnover.formRule.validate(Map.empty) must
        be(Invalid(Seq((Path \ "expectedAMLSTurnover", Seq(ValidationError("error.required.ba.turnover.from.mlr"))))))
    }
  }

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

      Json.toJson(ExpectedAMLSTurnover.First) must
        be(Json.obj("expectedAMLSTurnover" -> "01"))

      Json.toJson(ExpectedAMLSTurnover.Second) must
        be(Json.obj("expectedAMLSTurnover" -> "02"))

      Json.toJson(ExpectedAMLSTurnover.Third) must
        be(Json.obj("expectedAMLSTurnover" -> "03"))

      Json.toJson(ExpectedAMLSTurnover.Fourth) must
        be(Json.obj("expectedAMLSTurnover" -> "04"))

      Json.toJson(ExpectedAMLSTurnover.Fifth) must
        be(Json.obj("expectedAMLSTurnover" -> "05"))

      Json.toJson(ExpectedAMLSTurnover.Sixth) must
        be(Json.obj("expectedAMLSTurnover" -> "06"))

      Json.toJson(ExpectedAMLSTurnover.Seventh) must
        be(Json.obj("expectedAMLSTurnover" -> "07"))
    }

    "throw error for invalid data" in {
      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "20")) must
        be(JsError(JsPath, play.api.data.validation.ValidationError("error.invalid")))
    }
  }
}
