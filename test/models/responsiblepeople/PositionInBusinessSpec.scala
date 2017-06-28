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

import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json._

class PositionInBusinessSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "PositionInBusiness" must {

      "successfully validate" in {
        PositionWithinBusiness.formRule.validate("01") must be (Valid(BeneficialOwner))
        PositionWithinBusiness.formRule.validate("02") must be (Valid(Director))
        PositionWithinBusiness.formRule.validate("03") must be (Valid(InternalAccountant))
        PositionWithinBusiness.formRule.validate("04") must be (Valid(NominatedOfficer))
        PositionWithinBusiness.formRule.validate("05") must be (Valid(Partner))
        PositionWithinBusiness.formRule.validate("06") must be (Valid(SoleProprietor))
        PositionWithinBusiness.formRule.validate("07") must be (Valid(DesignatedMember))
      }

      "fail to validate an empty string" in {
        PositionWithinBusiness.formRule.validate("") must
          be(Invalid(Seq(
            (Path \ "positions") -> Seq(ValidationError("error.invalid"))
          )))
      }

      "fail to validate an invalid string" in {
        PositionWithinBusiness.formRule.validate("10") must
          be(Invalid(Seq(
            (Path \ "positions") -> Seq(ValidationError("error.invalid"))
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
  }

}
