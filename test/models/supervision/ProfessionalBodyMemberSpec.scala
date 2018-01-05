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

package models.supervision

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}


class ProfessionalBodyMemberSpec extends PlaySpec with MockitoSugar {

  "ProfessionalBodyMember" must {

    "pass validation" when {
      "more than one check box is selected" in {

        val model = Map("isAMember" -> Seq("true"))

        ProfessionalBodyMember.formRule.validate(model) must be(Valid(ProfessionalBodyMemberYes))

      }

      "'No' is selected" in {

        val model = Map("isAMember" -> Seq("false"))

        ProfessionalBodyMember.formRule.validate(model) must be(Valid(ProfessionalBodyMemberNo))

      }
    }

    "fail validation" when {
      "'isAMember' field field is missing" in {

        val model = Map[String, Seq[String]]()

        ProfessionalBodyMember.formRule.validate(model) must
          be(Invalid(List((Path \ "isAMember", Seq(ValidationError("error.required.supervision.business.a.member"))))))

      }

      "given no data represented by an empty Map" in {

        ProfessionalBodyMember.formRule.validate(Map.empty) must
          be(Invalid(Seq((Path \ "isAMember") -> Seq(ValidationError("error.required.supervision.business.a.member")))))

      }

    }

    "validate form write for option No" in {

      val map = Map("isAMember" -> Seq("false"))

      ProfessionalBodyMember.formWrites.writes(ProfessionalBodyMemberNo) must be (map)
    }

    "validate form write for option Yes" in {

      val map = Map("isAMember" -> Seq("true"))

      ProfessionalBodyMember.formWrites.writes(ProfessionalBodyMemberYes) must be (map)
    }

    "form write test" in {
      val map = Map("isAMember" -> Seq("false"))

      ProfessionalBodyMember.formWrites.writes(ProfessionalBodyMemberNo) must be(map)
    }

    "JSON validation" must {

      "successfully validate given values" in {
        val json =  Json.obj("isAMember" -> true)

        Json.fromJson[ProfessionalBodyMember](json) must be(JsSuccess(ProfessionalBodyMemberYes, JsPath))
      }

      "successfully validate given values with option No" in {
        val json =  Json.obj("isAMember" -> false)

        Json.fromJson[ProfessionalBodyMember](json) must be(JsSuccess(ProfessionalBodyMemberNo, JsPath))
      }

      "fail when on path is missing" in {
        Json.fromJson[ProfessionalBodyMember](Json.obj()) must
          be(JsError((JsPath \"isAMember") -> play.api.data.validation.ValidationError("error.path.missing")))
      }

      "fail when on invalid data" in {
        Json.fromJson[ProfessionalBodyMember](Json.obj("isAMember" -> "")) must
          be(JsError((JsPath \ "isAMember") -> play.api.data.validation.ValidationError("error.expected.jsboolean")))
      }

      "write valid data in using json write" in {
        Json.toJson[ProfessionalBodyMember](ProfessionalBodyMemberYes) must be (Json.obj("isAMember" -> true))
      }

      "write valid data in using json write with Option No" in {
        Json.toJson[ProfessionalBodyMember](ProfessionalBodyMemberNo) must be (Json.obj("isAMember" -> false))
      }
    }
  }
}