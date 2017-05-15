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

package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ProfessionalBodySpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {


    "pass validation" when {
      "given a valid enum value" in {

        ProfessionalBody.formRule.validate(Map("penalised" -> Seq("false"))) must
          be(Valid(ProfessionalBodyNo))
      }

      "given a `Yes` value" in {

        val data = Map(
          "penalised" -> Seq("true"),
          "professionalBody" -> Seq("details")
        )

        ProfessionalBody.formRule.validate(data) must
          be(Valid(ProfessionalBodyYes("details")))
      }
    }

    "fail validation" when {

      "missing mandatory value" in {

        ProfessionalBody.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "penalised") -> Seq(ValidationError("error.required.eab.penalised.by.professional.body"))
          )))
      }

      "given a `Yes` with an empty string" in {

        val data = Map(
          "penalised" -> Seq("true"),
          "professionalBody" -> Seq("")
        )

        ProfessionalBody.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "professionalBody") -> Seq(ValidationError("error.required.eab.info.about.penalty"))
          )))
      }

      "given a `Yes` with whitespace only" in {

        val data = Map(
          "penalised" -> Seq("true"),
          "professionalBody" -> Seq("    ")
        )

        ProfessionalBody.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "professionalBody") -> Seq(ValidationError("error.required.eab.info.about.penalty"))
          )))
      }

      "given a `Yes` with too many characters" in {

        val data = Map(
          "penalised" -> Seq("true"),
          "professionalBody" -> Seq("zzxczxczx" * 50)
        )

        ProfessionalBody.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "professionalBody") -> Seq(ValidationError("error.invalid.maxlength.255"))
          )))
      }

      "given a `Yes` with invalid characters" in {

        val data = Map(
          "penalised" -> Seq("true"),
          "professionalBody" -> Seq("{}{}}")
        )

        ProfessionalBody.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "professionalBody") -> Seq(ValidationError("err.text.validation"))
          )))
      }
    }

    "write correct data from enum value" in {

      ProfessionalBody.formWrites.writes(ProfessionalBodyNo) must
        be(Map("penalised" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      ProfessionalBody.formWrites.writes(ProfessionalBodyYes("details")) must
        be(Map("penalised" -> Seq("true"), "professionalBody" -> Seq("details")))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[ProfessionalBody](Json.obj("penalised" -> false)) must
        be(JsSuccess(ProfessionalBodyNo, JsPath ))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("penalised" -> true, "professionalBody" ->"details")

      Json.fromJson[ProfessionalBody](json) must
        be(JsSuccess(ProfessionalBodyYes("details"), JsPath \ "professionalBody"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("penalised" -> true)

      Json.fromJson[ProfessionalBody](json) must
        be(JsError((JsPath \ "professionalBody") -> play.api.data.validation.ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(ProfessionalBodyNo) must
        be(Json.obj("penalised" -> false))

      Json.toJson(ProfessionalBodyYes("details")) must
        be(Json.obj(
          "penalised" -> true,
          "professionalBody" -> "details"
        ))
    }
  }


}
