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

package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}


class PenalisedUnderEstateAgentsActSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {
      PenalisedUnderEstateAgentsAct.formRule.validate(Map("penalisedUnderEstateAgentsAct" -> Seq("false"))) must
        be(Valid(PenalisedUnderEstateAgentsActNo))
    }

    "successfully validate given an `Yes` value" in {
      val data = Map(
        "penalisedUnderEstateAgentsAct" -> Seq("true"),
        "penalisedUnderEstateAgentsActDetails" -> Seq("Do not remember why penalised before")
      )

      PenalisedUnderEstateAgentsAct.formRule.validate(data) must
        be(Valid(PenalisedUnderEstateAgentsActYes("Do not remember why penalised before")))
    }

    "fail to validate an invalid string" in {
      val data = Map(
        "penalisedUnderEstateAgentsAct" -> Seq("true"),
        "penalisedUnderEstateAgentsActDetails" -> Seq("{}")
      )


      PenalisedUnderEstateAgentsAct.formRule.validate(data) must
        be(Invalid(Seq(
          (Path \ "penalisedUnderEstateAgentsActDetails") -> Seq(ValidationError("err.text.validation"))
        )))
    }


    "fail to validate given mandatory field" in {

      PenalisedUnderEstateAgentsAct.formRule.validate(Map.empty) must
        be(Invalid(Seq(
          (Path \ "penalisedUnderEstateAgentsAct") -> Seq(ValidationError("error.required.eab.penalised.under.act"))
        )))
    }


    "fail to validate given a `Yes` but no details provided" in {
      val data = Map(
        "penalisedUnderEstateAgentsAct" -> Seq("true"),
        "penalisedUnderEstateAgentsActDetails" -> Seq("")
      )

      PenalisedUnderEstateAgentsAct.formRule.validate(data) must
        be(Invalid(Seq(
          (Path \ "penalisedUnderEstateAgentsActDetails") -> Seq(ValidationError("error.required.eab.info.about.penalty"))
        )))
    }

    "fail to validate given a `Yes` but max details provided" in {
      val data = Map(
        "penalisedUnderEstateAgentsAct" -> Seq("true"),
        "penalisedUnderEstateAgentsActDetails" -> Seq("a"*256)
      )

      PenalisedUnderEstateAgentsAct.formRule.validate(data) must
        be(Invalid(Seq(
          (Path \ "penalisedUnderEstateAgentsActDetails") -> Seq(ValidationError("error.invalid.maxlength.255"))
        )))
    }

    "write correct data from enum value" in {
      PenalisedUnderEstateAgentsAct.formWrites.writes(PenalisedUnderEstateAgentsActNo) must
        be(Map("penalisedUnderEstateAgentsAct" -> Seq("false")))
    }

    "write correct data from `Yes` value" in {
      PenalisedUnderEstateAgentsAct.formWrites.writes(PenalisedUnderEstateAgentsActYes("Do not remember why penalised before")) must
        be(Map("penalisedUnderEstateAgentsAct" -> Seq("true"), "penalisedUnderEstateAgentsActDetails" -> Seq("Do not remember why penalised before")))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {
      Json.fromJson[PenalisedUnderEstateAgentsAct](Json.obj("penalisedUnderEstateAgentsAct" -> false)) must
        be(JsSuccess(PenalisedUnderEstateAgentsActNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {
      val json = Json.obj("penalisedUnderEstateAgentsAct" -> true, "penalisedUnderEstateAgentsActDetails" -> "Do not remember why penalised before")
      Json.fromJson[PenalisedUnderEstateAgentsAct](json) must
        be(JsSuccess(PenalisedUnderEstateAgentsActYes("Do not remember why penalised before"),
          JsPath \ "penalisedUnderEstateAgentsActDetails"))
    }

    "fail to validate when given an empty `Yes` value" in {
      val json = Json.obj("penalisedUnderEstateAgentsAct" -> true)
      Json.fromJson[PenalisedUnderEstateAgentsAct](json) must
        be(JsError((JsPath \ "penalisedUnderEstateAgentsActDetails") -> play.api.data.validation.ValidationError("error.path.missing")))
    }

    "write the correct value" in {
      Json.toJson(PenalisedUnderEstateAgentsActNo) must be(Json.obj("penalisedUnderEstateAgentsAct" -> false))
      Json.toJson(PenalisedUnderEstateAgentsActYes("Do not remember why penalised before")) must
        be(Json.obj(
          "penalisedUnderEstateAgentsAct" -> true,
          "penalisedUnderEstateAgentsActDetails" -> "Do not remember why penalised before"
        ))
    }
  }

}
