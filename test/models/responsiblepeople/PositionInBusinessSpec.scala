/*
 * Copyright 2019 HM Revenue & Customs
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

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class PositionInBusinessSpec extends PlaySpec with MockitoSugar {

  "PositionInBusiness" must {

    "validate position and 'other' text from tuple" in {
      PositionWithinBusiness.fullySpecifiedRule.validate((Set("01"), None)) mustBe Valid(Set(BeneficialOwner))
      PositionWithinBusiness.fullySpecifiedRule.validate((Set("02"), None)) mustBe Valid(Set(Director))
      PositionWithinBusiness.fullySpecifiedRule.validate((Set("03"), None)) mustBe Valid(Set(InternalAccountant))
      PositionWithinBusiness.fullySpecifiedRule.validate((Set("04"), None)) mustBe Valid(Set(NominatedOfficer))
      PositionWithinBusiness.fullySpecifiedRule.validate((Set("05"), None)) mustBe Valid(Set(Partner))
      PositionWithinBusiness.fullySpecifiedRule.validate((Set("06"), None)) mustBe Valid(Set(SoleProprietor))
      PositionWithinBusiness.fullySpecifiedRule.validate((Set("07"), None)) mustBe Valid(Set(DesignatedMember))
      PositionWithinBusiness.fullySpecifiedRule.validate((Set("other"), Some("some other role"))) mustBe Valid(Set(Other("some other role")))
    }

    "successfully validate form" in {
      val form = Map(
        "positions[0]" -> Seq("01"),
        "positions[1]" -> Seq("other"),
        "otherPosition" -> Seq("some other position"))

      PositionWithinBusiness.positionsRule.validate(form) mustBe
        Valid( Set(BeneficialOwner, Other("some other position")))
    }

    "fail to validate when 'other' is selected but no 'other' value is given" in {
      val form = Map(
        "positions[0]" -> Seq("01"),
        "positions[1]" -> Seq("other"))

      PositionWithinBusiness.positionsRule.validate(form) mustBe
        Invalid(Seq((Path \ "otherPosition") -> Seq(ValidationError("responsiblepeople.position_within_business.other_position.othermissing"))))
    }

    "fail to validate when an invalid valid was given" in {
      val form = Map(
        "positions[0]" -> Seq("10")
      )

      intercept[Exception] {
        PositionWithinBusiness.positionsRule.validate(form)
      }
    }

    "fail to validate an empty position list" in {
      PositionWithinBusiness.atLeastOneRule.validate(Set.empty[String]) must
        be(Invalid(Seq(
          Path -> Seq(ValidationError("error.required.positionWithinBusiness"))
        )))
    }

    "write correct position id" in {
      PositionWithinBusiness.formWrite.writes(BeneficialOwner) must be("01")
      PositionWithinBusiness.formWrite.writes(Director) must be("02")
      PositionWithinBusiness.formWrite.writes(InternalAccountant) must be("03")
      PositionWithinBusiness.formWrite.writes(NominatedOfficer) must be("04")
      PositionWithinBusiness.formWrite.writes(Partner) must be("05")
      PositionWithinBusiness.formWrite.writes(SoleProprietor) must be("06")
      PositionWithinBusiness.formWrite.writes(DesignatedMember) must be("07")
      PositionWithinBusiness.formWrite.writes(Other("")) must be("other")
    }

    "successfully write form from a set of PositionWithinBusiness" in {
      val model = Set(InternalAccountant, Other("some other position")).asInstanceOf[Set[PositionWithinBusiness]]
      PositionWithinBusiness.formWrites.writes(model) mustBe Map(
        "positions[]" -> Seq("03", "other"),
        "otherPosition" -> Seq("some other position"))
    }

    "JSON validation" must {

      "read the correct value" when {

        "given a BeneficialOwner value" in {
          Json.fromJson[PositionWithinBusiness](JsString("01")) must
            be(JsSuccess(BeneficialOwner))
        }

        "given a Director value" in {
          Json.fromJson[PositionWithinBusiness](JsString("02")) must
            be(JsSuccess(Director))
        }

        "given a InternalAccountant value" in {
          Json.fromJson[PositionWithinBusiness](JsString("03")) must
            be(JsSuccess(InternalAccountant))
        }

        "given a NominatedOfficer value" in {
          Json.fromJson[PositionWithinBusiness](JsString("04")) must
            be(JsSuccess(NominatedOfficer))
        }

        "given a Partner value" in {
          Json.fromJson[PositionWithinBusiness](JsString("05")) must
            be(JsSuccess(Partner))
        }

        "given a SoleProprietor value" in {
          Json.fromJson[PositionWithinBusiness](JsString("06")) must
            be(JsSuccess(SoleProprietor))
        }

        "given a DesignatedMember value" in {
          Json.fromJson[PositionWithinBusiness](JsString("07")) must
            be(JsSuccess(DesignatedMember))
        }

        "given an OtherSelection value" in {
          Json.fromJson[PositionWithinBusiness](Json.obj("other" -> "some other role")) mustBe JsSuccess(Other("some other role"))
        }
      }

      "fail to validate" when {
        "given an empty value" in {
          Json.fromJson[PositionWithinBusiness](JsString("")) must
            be(JsError((JsPath \ "positions") -> play.api.libs.json.JsonValidationError("error.invalid")))
        }
      }

      "write the correct value" when {

        "given a BeneficialOwner" in {
          Json.toJson(BeneficialOwner) must be(JsString("01"))
        }

        "given a Director" in {
          Json.toJson(Director) must be(JsString("02"))
        }

        "given a InternalAccountant" in {
          Json.toJson(InternalAccountant) must be(JsString("03"))
        }

        "given a NominatedOfficer" in {
          Json.toJson(NominatedOfficer) must be(JsString("04"))
        }

        "given a Partner" in {
          Json.toJson(Partner) must be(JsString("05"))
        }

        "given a SoleProprietor" in {
          Json.toJson(SoleProprietor) must be(JsString("06"))
        }

        "given a DesignatedMember" in {
          Json.toJson(DesignatedMember) must be(JsString("07"))
        }

        "given an Other" in {
          Json.toJson(Other("some new role")) mustBe Json.obj("other" -> "some new role")
        }
      }
    }
  }
}
