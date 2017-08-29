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

package models.responsiblepeople

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class PositionInBusinessSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "PositionInBusiness" must {

      "successfully validate" in {
        PositionWithinBusiness.formRule.validate((Set("01"), None)) mustBe Valid(Set(BeneficialOwner))
        PositionWithinBusiness.formRule.validate((Set("02"), None)) mustBe Valid(Set(Director))
        PositionWithinBusiness.formRule.validate((Set("03"), None)) mustBe Valid(Set(InternalAccountant))
        PositionWithinBusiness.formRule.validate((Set("04"), None)) mustBe Valid(Set(NominatedOfficer))
        PositionWithinBusiness.formRule.validate((Set("05"), None)) mustBe Valid(Set(Partner))
        PositionWithinBusiness.formRule.validate((Set("06"), None)) mustBe Valid(Set(SoleProprietor))
        PositionWithinBusiness.formRule.validate((Set("07"), None)) mustBe Valid(Set(DesignatedMember))
        PositionWithinBusiness.formRule.validate((Set("other"), Some("some other role"))) mustBe Valid(Set(Other("some other role")))
      }

      "fail to validate an empty string" in {
        Positions.positionReader.validate(Set.empty[String]) must
          be(Invalid(Seq(
            Path -> Seq(ValidationError("error.required.positionWithinBusiness"))
          )))
      }
    }

    "write correct data from enum value" in {
      PositionWithinBusiness.formWrite.writes(BeneficialOwner) must be("01")
      PositionWithinBusiness.formWrite.writes(Director) must be("02")
      PositionWithinBusiness.formWrite.writes(InternalAccountant) must be("03")
      PositionWithinBusiness.formWrite.writes(NominatedOfficer) must be("04")
      PositionWithinBusiness.formWrite.writes(Partner) must be("05")
      PositionWithinBusiness.formWrite.writes(SoleProprietor) must be("06")
      PositionWithinBusiness.formWrite.writes(DesignatedMember) must be("07")
      PositionWithinBusiness.formWrite.writes(Other("")) must be("other")
    }

  }

  "Positions" must {
    "successfully read whole form" in {
      val form = Map(
        "positions[0]" -> Seq("01"),
        "positions[1]" -> Seq("other"),
        "otherPosition" -> Seq("some other position"),
        "startDate.day" -> Seq("1"),
        "startDate.month" -> Seq("1"),
        "startDate.year" -> Seq("1970")
      )

      //noinspection ScalaStyle
      Positions.formReads.validate(form) mustBe
        Valid(Positions(
          Set(BeneficialOwner, Other("some other position")),
          Some(new LocalDate(1970, 1, 1))))
    }

    "successfully write whole form" in {
      //noinspection ScalaStyle
      val model = Positions(Set(InternalAccountant, Other("some other position")), Some(new LocalDate(1999, 5, 1)))

      Positions.formWrites.writes(model) mustBe Map(
        "positions[]" -> Seq("03", "other"),
        "otherPosition" -> Seq("some other position"),
        "startDate.day" -> Seq("1"),
        "startDate.month" -> Seq("5"),
        "startDate.year" -> Seq("1999")
      )
    }

    "fail to validate when no 'other' value is given" in {
      val form = Map(
        "positions[0]" -> Seq("01"),
        "positions[1]" -> Seq("other"),
        "startDate.day" -> Seq("1"),
        "startDate.month" -> Seq("1"),
        "startDate.year" -> Seq("1970")
      )

      Positions.formReads.validate(form) mustBe
        Invalid(Seq((Path \ "otherPosition") -> Seq(ValidationError("responsiblepeople.position_within_business.other_position.othermissing"))))
    }

    "fail to validate when an invalid valid was given" in {
      val form = Map(
        "positions[0]" -> Seq("10"),
        "startDate.day" -> Seq("1"),
        "startDate.month" -> Seq("1"),
        "startDate.year" -> Seq("1970")
      )

      intercept[Exception] {
        Positions.formReads.validate(form)
      }
    }
  }

  "Positions hasNominatedOfficer" must {

    "return true when there is a nominated officer RP" in {
      val positions = Positions(Set(NominatedOfficer,InternalAccountant),Some(new LocalDate()))
      positions.isNominatedOfficer must be(true)

    }

    "return false when there is no nominated officer RP" in {
      val positions = Positions(Set(InternalAccountant),Some(new LocalDate()))
      positions.isNominatedOfficer must be(false)

    }
  }

  "JSON validation" must {

    "convert to json" in {
      val model = Positions(Set(BeneficialOwner, Other("some other role")), Some(new LocalDate(1970, 1, 1)))

      Json.toJson(model) mustBe Json.obj(
        "positions" -> JsArray(Seq(JsString("01"), Json.obj("other" -> "some other role"))),
        "startDate" -> "1970-01-01"
      )
    }

    "successfully validate given a BeneficialOwner value" in {
      Json.fromJson[PositionWithinBusiness](JsString("01")) must
        be(JsSuccess(BeneficialOwner))
    }

    "successfully validate given a Director value" in {
      Json.fromJson[PositionWithinBusiness](JsString("02")) must
        be(JsSuccess(Director))
    }

    "successfully validate given a InternalAccountant value" in {
      Json.fromJson[PositionWithinBusiness](JsString("03")) must
        be(JsSuccess(InternalAccountant))
    }

    "successfully validate given a NominatedOfficer value" in {
      Json.fromJson[PositionWithinBusiness](JsString("04")) must
        be(JsSuccess(NominatedOfficer))
    }

    "successfully validate given a Partner value" in {
      Json.fromJson[PositionWithinBusiness](JsString("05")) must
        be(JsSuccess(Partner))
    }

    "successfully validate given a SoleProprietor value" in {
      Json.fromJson[PositionWithinBusiness](JsString("06")) must
        be(JsSuccess(SoleProprietor))
    }

    "successfully validate given a DesignatedMember value" in {
      Json.fromJson[PositionWithinBusiness](JsString("07")) must
        be(JsSuccess(DesignatedMember))
    }

    "successfully validate given an OtherSelection value" in {
      Json.fromJson[PositionWithinBusiness](Json.obj("other" -> "some other role")) mustBe JsSuccess(Other("some other role"))
    }

    "fail to validate when given an empty value" in {
      Json.fromJson[PositionWithinBusiness](JsString("")) must
        be(JsError((JsPath \ "positions") -> play.api.data.validation.ValidationError("error.invalid")))
    }

    "write the correct value for BeneficialOwner" in {
      Json.toJson(BeneficialOwner) must be(JsString("01"))
    }

    "write the correct value for Director" in {
      Json.toJson(Director) must be(JsString("02"))
    }

    "write the correct value for InternalAccountant" in {
      Json.toJson(InternalAccountant) must be(JsString("03"))
    }

    "write the correct value for NominatedOfficer" in {
      Json.toJson(NominatedOfficer) must be(JsString("04"))
    }

    "write the correct value for Partner" in {
      Json.toJson(Partner) must be(JsString("05"))
    }

    "write the correct value for SoleProprietor" in {
      Json.toJson(SoleProprietor) must be(JsString("06"))
    }

    "write the correct value for DesignatedMember" in {
      Json.toJson(DesignatedMember) must be(JsString("07"))
    }

    "write the correct value for Other" in {
      Json.toJson(Other("some new role")) mustBe Json.obj("other" -> "some new role")
    }
  }

}
