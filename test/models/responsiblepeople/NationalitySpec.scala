/*
 * Copyright 2020 HM Revenue & Customs
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

package models.responsiblepeople

import cats.data.Validated.Valid
import models.Country
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}


class NationalitySpec extends PlaySpec with MockitoSugar {

  "Form:" must {

    "successfully pass validation for British" in {
      val urlFormEncoded = Map("nationality" -> Seq("01"))
      Nationality.formRule.validate(urlFormEncoded) must be(Valid(British))
    }

    "successfully pass validation for otherCountry" in {
      val urlFormEncoded = Map(
        "nationality" -> Seq("02"),
        "otherCountry" -> Seq("GB")
      )
      Nationality.formRule.validate(urlFormEncoded) must be(Valid(OtherCountry(Country("United Kingdom", "GB"))))
    }

    "successfully validate when the country is typed and not selected and is valid" in {
      val urlFormEncoded = Map(
        "nationality" -> Seq("02"),
        "otherCountry" -> Seq("albania")
      )
      Nationality.formRule.validate(urlFormEncoded) must be(Valid(OtherCountry(Country("Albania", "AL"))))
    }

    "fail to validate when the country is typed and not selected and is not valid" in {
      val urlFormEncoded = Map(
        "nationality" -> Seq("02"),
        "otherCountry" -> Seq("albanias")
      )

      Nationality.formRule.validate(urlFormEncoded).toString.contains("error.invalid.rp.nationality.country").mustBe(true)
    }

    "fail validation if not Other value" in {
      val urlFormEncoded = Map(
        "nationality" -> Seq("02"),
        "otherCountry" -> Seq("")
      )
      Nationality.formRule.validate(urlFormEncoded) must be(Invalid(Seq(
        (Path \ "otherCountry") -> Seq(ValidationError("error.required.rp.nationality.country"))
      )))
    }

    "fail validation if invalid Other value" in {
      val urlFormEncoded = Map(
        "nationality" -> Seq("02"),
        "otherCountry" -> Seq("invalid")
      )
      Nationality.formRule.validate(urlFormEncoded) must be(Invalid(Seq(
        (Path \ "otherCountry") -> Seq(ValidationError("error.invalid.rp.nationality.country"))
      )))
    }

    "fail validation when user has not selected at least one of the options" in {
      Nationality.formRule.validate(Map.empty) must be(Invalid(Seq(
        (Path \ "nationality") -> Seq(ValidationError("error.required.nationality"))
      )))
    }

    "fail to validate given an invalid value supplied that is not matching to any nationality" in {

      val urlFormEncoded = Map("nationality" -> Seq("10"))

      Nationality.formRule.validate(urlFormEncoded) must
        be(Invalid(Seq(
          (Path \ "nationality") -> Seq(ValidationError("error.invalid"))
        )))
    }

    "When the user loads the page and that is posted to the form, the nationality" must {

      "load the correct value in the form for British" in {
        Nationality.formWrite.writes(British) must be(Map("nationality" -> Seq("01")))
      }

      "load the correct value in the form for Other value" in {
        Nationality.formWrite.writes(OtherCountry(Country("United Kingdom", "GB"))) must be(Map(
          "nationality" -> Seq("02"),
          "otherCountry" -> Seq("GB")
        ))
      }
    }
  }

  "JSON" must {

    "Read json and write the option British successfully" in {

      Nationality.jsonReads.reads(Nationality.jsonWrites.writes(British)) must be(JsSuccess(British, JsPath))
    }

    "Read read and write the option `other country` successfully" in {
      val json = Nationality.jsonWrites.writes(OtherCountry(Country("United Kingdom", "GB")))
      Nationality.jsonReads.reads(json) must be(
        JsSuccess(OtherCountry(Country("United Kingdom", "GB")), JsPath \ "otherCountry"))
    }

    "fail to validate given an invalid value supplied that is not matching to any nationality" in {

      Nationality.jsonReads.reads(Json.obj("nationality" -> "10")) must be(JsError(List((JsPath, List(play.api.libs.json.JsonValidationError("error.invalid"))))))

    }
  }
}
