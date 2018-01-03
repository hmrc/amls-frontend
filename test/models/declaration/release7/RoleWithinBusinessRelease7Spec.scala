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

package models.declaration.release7

import jto.validation.{Invalid, Path, Valid, ValidationError}
import models.declaration.RoleWithinBusiness
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import play.api.test.FakeApplication


class RoleWithinBusinessRelease7Spec extends PlaySpec with MockitoSugar with OneAppPerSuite{


  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> true))

  "RoleWithinBusiness" must {

    "validate model with few check box selected" in {

      val model = Map(
        "positions[]" -> Seq("01", "02" ,"other"),
        "otherPosition" -> Seq("test")
      )

      RoleWithinBusinessRelease7.formRule.validate(model) must
        be(Valid(RoleWithinBusinessRelease7(Set(
          BeneficialShareholder,
          Director,
          Other("test")
        ))))

    }

    "fail validation when 'Other' is selected but no details are provided" when {
      "represented by an empty string" in {
        val model = Map("positions[]" -> Seq("other"),
          "otherPosition" -> Seq(""))

        RoleWithinBusinessRelease7.formRule.validate(model) must
          be(Invalid(List((Path \ "otherPosition", Seq(ValidationError("error.required.declaration.specify.role"))))))
      }

      "represented by a sequence of whitespace" in {
        val model = Map("positions[]" -> Seq("other"),
          "otherPosition" -> Seq("  \t"))

        RoleWithinBusinessRelease7.formRule.validate(model) must
          be(Invalid(List((Path \ "otherPosition", Seq(ValidationError("error.required.declaration.specify.role"))))))
      }

      "represented by a missing field" in {
        val model = Map("positions[]" -> Seq("other"))
        RoleWithinBusinessRelease7.formRule.validate(model) must
          be(Invalid(List((Path \ "otherPosition", Seq(ValidationError("error.required"))))))
      }
    }

    "fail validation when field otherDetails exceeds maximum length" in {

      val model = Map(
        "positions[]" -> Seq(
          "01",
          "02",
          "05",
          "03",
          "06",
          "other"
        ), "otherPosition" -> Seq("t"*256)
      )
      RoleWithinBusinessRelease7.formRule.validate(model) must
        be(Invalid(List(( Path \ "otherPosition", Seq(ValidationError("error.invalid.maxlength.255"))))))
    }


    "fail validation when none of the check boxes are selected" when {
      List(
        "empty list" -> Map("positions[]" -> Seq(),"otherPosition" -> Seq("test")),
        "missing field" -> Map.empty[String, Seq[String]]
      ).foreach { x =>
        val (rep, model) = x
        s"represented by $rep" in {
          RoleWithinBusinessRelease7.formRule.validate(model) must
            be(Invalid(List((Path \ "positions", List(ValidationError("error.required"))))))
        }
      }
    }

    "fail to validate invalid data" in {
      val model = Map(
        "positions[]" -> Seq("01, dfdfdfdf")
      )
      RoleWithinBusinessRelease7.formRule.validate(model) must
        be(Invalid(Seq((Path \ "positions") -> Seq(ValidationError("error.invalid")))))

    }

    "validate form write for valid transaction record" in {

      val map = Map(
        "positions[]" -> Seq("other","02"),
        "otherPosition" -> Seq("test")
      )

      val model = RoleWithinBusinessRelease7(Set(Other("test"), Director))
      RoleWithinBusinessRelease7.formWrites.writes(model) must be (map)
    }

    "validate form write for multiple options" in {

      val map = Map(
        "positions[]" -> List("08","05","06","07","01")
      )

      val model = RoleWithinBusinessRelease7(Set(BeneficialShareholder, SoleProprietor, Partner, DesignatedMember, ExternalAccountant))
      RoleWithinBusinessRelease7.formWrites.writes(model) must be (map)
    }

    "JSON validation" must {

      "successfully convert release 6 data to release 7 model" in {
        val json =  Json.obj(
          "roleWithinBusiness" -> "01")

        Json.fromJson[RoleWithinBusinessRelease7](json) must
          be(JsSuccess(RoleWithinBusinessRelease7(Set(BeneficialShareholder)), JsPath))
      }

      "successfully validate given values" in {
        val json =  Json.obj(
          "roleWithinBusiness" -> Seq("SoleProprietor","NominatedOfficer", "DesignatedMember", "Director", "BeneficialShareholder"))

        Json.fromJson[RoleWithinBusinessRelease7](json) must
          be(JsSuccess(RoleWithinBusinessRelease7(Set(SoleProprietor, NominatedOfficer, DesignatedMember, Director, BeneficialShareholder)), JsPath))
      }
      "successfully validate given all values" in {
        val json =  Json.obj(
          "roleWithinBusiness" -> Seq("BeneficialShareholder","Director","Partner","InternalAccountant","ExternalAccountant",
            "SoleProprietor","NominatedOfficer","DesignatedMember", "Other"),
        "roleWithinBusinessOther" -> "some other text")



        Json.fromJson[RoleWithinBusinessRelease7](json) must
          be(JsSuccess(RoleWithinBusinessRelease7(Set(
            BeneficialShareholder,
            Director,
            Partner,
            InternalAccountant,
            ExternalAccountant,
            SoleProprietor,
            NominatedOfficer,
            DesignatedMember,
            Other("some other text"))), JsPath))
      }

      "successfully validate given values with option other details" in {
        val json =  Json.obj(
          "roleWithinBusiness" -> Seq("DesignatedMember", "Other"),
          "roleWithinBusinessOther" -> "test")

        Json.fromJson[RoleWithinBusinessRelease7](json) must
          be(JsSuccess(RoleWithinBusinessRelease7(Set(Other("test"), DesignatedMember)), JsPath))
      }

      "fail when path is missing" in {
        Json.fromJson[RoleWithinBusinessRelease7](Json.obj(
          "roleWithinBusinessOther" -> "other text")) must
          be(JsError((JsPath \ "roleWithinBusiness") -> play.api.data.validation.ValidationError("error.path.missing")))
      }

      "fail when on invalid data" in {
        Json.fromJson[RoleWithinBusinessRelease7](Json.obj("roleWithinBusiness" -> Set("hello"))) must
          be(JsError((JsPath \ "roleWithinBusiness") -> play.api.data.validation.ValidationError("error.invalid")))
      }

      "write valid data in using json write" in {
        val release = RoleWithinBusinessRelease7(Set(SoleProprietor, Other("test657")))

        Json.toJson[RoleWithinBusinessRelease7](release) must be (
          Json.obj("roleWithinBusiness" -> Json.arr("SoleProprietor", "Other"),
            "roleWithinBusinessOther" -> "test657"
          ))
      }
    }
  }

  "otherDetailsType" must {
    "pass validation" when {
      "255 valid characters are entered" in {
        RoleWithinBusinessRelease7.otherDetailsType.validate("1" * 255) must be(Valid("1" * 255))
      }
    }

    "fail validation" when {
      "more than 255 characters are entered" in {
        RoleWithinBusinessRelease7.otherDetailsType.validate("1" * 256) must be(
          Invalid(Seq(Path -> Seq(ValidationError("error.invalid.maxlength.255")))))
      }
      "given an empty string" in {
        RoleWithinBusinessRelease7.otherDetailsType.validate("") must be(
          Invalid(Seq(Path -> Seq(ValidationError("error.required.declaration.specify.role")))))
      }
      "given whitespace only" in {
        RoleWithinBusinessRelease7.otherDetailsType.validate("     ") must be(
          Invalid(Seq(Path -> Seq(ValidationError("error.required.declaration.specify.role")))))
      }
      "given invalid characters" in {
        RoleWithinBusinessRelease7.otherDetailsType.validate("{}") must be(
          Invalid(Seq(Path -> Seq(ValidationError("err.text.validation")))))
      }
    }
  }
}

