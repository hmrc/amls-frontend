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

        val model = Map(
          "isAMember" -> Seq("true"),
          "businessType[]" -> Seq("01", "02", "14"),
          "specifyOtherBusiness" -> Seq("test")
        )

        ProfessionalBodyMember.formRule.validate(model) must
          be(Valid(ProfessionalBodyMemberYes(Set(AccountingTechnicians, CharteredCertifiedAccountants, Other("test")))))

      }

      "'No' is selected" in {

        val model = Map(
          "isAMember" -> Seq("false")
        )

        ProfessionalBodyMember.formRule.validate(model) must
          be(Valid(ProfessionalBodyMemberNo))

      }
    }

    "fail validation" when {
      "'isAMember' field field is missing" in {

        val model = Map(
          "businessType[]" -> Seq("01", "02", "03"),
          "specifyOtherBusiness" -> Seq("")
        )

        ProfessionalBodyMember.formRule.validate(model) must
          be(Invalid(List((Path \ "isAMember", Seq(ValidationError("error.required.supervision.business.a.member"))))))

      }

      "'isAMember is set to 'true' and 'Other' is selected, but specifyOtherBusiness is an empty string" in {

        val model = Map(
          "isAMember" -> Seq("true"),
          "businessType[]" -> Seq("01", "02", "14"),
          "specifyOtherBusiness" -> Seq("")
        )
        ProfessionalBodyMember.formRule.validate(model) must
          be(Invalid(List((Path \ "specifyOtherBusiness", Seq(ValidationError("error.required.supervision.business.details"))))))
      }

      "specifyOtherBusiness exceeds max length" in {

        val model = Map(
          "isAMember" -> Seq("true"),
          "businessType[]" -> Seq("01", "02", "14"),
          "specifyOtherBusiness" -> Seq("test" * 200)
        )
        ProfessionalBodyMember.formRule.validate(model) must
          be(Invalid(List((Path \ "specifyOtherBusiness", Seq(ValidationError("error.invalid.supervision.business.details"))))))
      }

      "'isAMember is set to 'true' but businessType[] is empty" in {

        val model = Map(
          "isAMember" -> Seq("true"),
          "businessType[]" -> Seq(),
          "specifyOtherBusiness" -> Seq("test")
        )
        ProfessionalBodyMember.formRule.validate(model) must
          be(Invalid(List((Path \ "businessType", Seq(ValidationError("error.required.supervision.one.professional.body"))))))
      }

      "given no data represented by an empty Map" in {

        ProfessionalBodyMember.formRule.validate(Map.empty) must
          be(Invalid(Seq((Path \ "isAMember") -> Seq(ValidationError("error.required.supervision.business.a.member")))))

      }

      "given invalid businessType[] selection" in {

        val model = Map(
          "isAMember" -> Seq("true"),
          "businessType[]" -> Seq("01", "20")
        )
        ProfessionalBodyMember.formRule.validate(model) must
          be(Invalid(Seq((Path \ "businessType") -> Seq(ValidationError("error.invalid")))))

      }

      "given invalid characters in specifyOther" in {

        val model = Map(
          "isAMember" -> Seq("true"),
          "businessType[]" -> Seq("14"),
          "specifyOtherBusiness" -> Seq("{}{}")
        )
        ProfessionalBodyMember.formRule.validate(model) must
        be(Invalid(Seq((Path \ "specifyOtherBusiness", Seq(ValidationError("err.text.validation"))))))
      }
    }
    "validate form write for valid transaction record" in {

      val map = Map(
        "isAMember" -> Seq("true"),
        "businessType[]" -> Seq("14","13"),
        "specifyOtherBusiness" -> Seq("test")
      )

      val model = ProfessionalBodyMemberYes(Set(Other("test"), LawSociety))

     ProfessionalBodyMember.formWrites.writes(model) must be (map)
    }

    "validate form write for option No" in {

      val map = Map(
        "isAMember" -> Seq("false")
      )
      val model = ProfessionalBodyMemberNo
      ProfessionalBodyMember.formWrites.writes(model) must be (map)
    }

    "validate form write for option Yes" in {

      val map = Map(
        "isAMember" -> Seq("true"),
        "businessType[]" -> Seq("03","04", "05", "06")
      )

      val model = ProfessionalBodyMemberYes(Set(InternationalAccountants,
        TaxationTechnicians, ManagementAccountants, InstituteOfTaxation))
      ProfessionalBodyMember.formWrites.writes(model) must be (map)
    }

    "form write test" in {
      val map = Map(
        "isAMember" -> Seq("false")
      )
      val model = ProfessionalBodyMemberNo

      ProfessionalBodyMember.formWrites.writes(model) must be(map)
    }

    "JSON validation" must {

      "successfully validate given values" in {
        val json =  Json.obj("isAMember" -> true,
          "businessType" -> Seq("01","02"))

        Json.fromJson[ProfessionalBodyMember](json) must
          be(JsSuccess(ProfessionalBodyMemberYes(Set(AccountingTechnicians, CharteredCertifiedAccountants)), JsPath))
      }

      "successfully validate given values with option No" in {
        val json =  Json.obj("isAMember" -> false)

        Json.fromJson[ProfessionalBodyMember](json) must
          be(JsSuccess(ProfessionalBodyMemberNo, JsPath))
      }

      "successfully validate given values with option Digital software" in {
        val json =  Json.obj("isAMember" -> true,
          "businessType" -> Seq("14", "12"),
        "specifyOtherBusiness" -> "test")

        Json.fromJson[ProfessionalBodyMember](json) must
          be(JsSuccess(ProfessionalBodyMemberYes(Set(Other("test"), AssociationOfBookkeepers)), JsPath ))
      }

      "fail when on path is missing" in {
        Json.fromJson[ProfessionalBodyMember](Json.obj("isAMember" -> true,
          "transaction" -> Seq("01"))) must
          be(JsError((JsPath \"businessType") -> play.api.data.validation.ValidationError("error.path.missing")))
      }

      "fail when on invalid data" in {
        Json.fromJson[ProfessionalBodyMember](Json.obj("isAMember" -> true,"businessType" -> Seq("40"))) must
          be(JsError(((JsPath ) \ "businessType") -> play.api.data.validation.ValidationError("error.invalid")))
      }

      "write valid data in using json write" in {
        Json.toJson[ProfessionalBodyMember](ProfessionalBodyMemberYes(Set(AccountantsScotland, Other("test657")))) must be (Json.obj("isAMember" -> true,
        "businessType" -> Seq("09", "14"),
          "specifyOtherBusiness" -> "test657"
        ))
      }

      "write valid data in using json write with Option No" in {
        Json.toJson[ProfessionalBodyMember](ProfessionalBodyMemberNo) must be (Json.obj("isAMember" -> false))
      }
    }
  }
}


