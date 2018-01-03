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

package models.declaration

import jto.validation.{Invalid, Path, Valid, ValidationError}
import models.CharacterSets
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import play.api.test.FakeApplication


class RoleWithinBusinessSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with CharacterSets {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> false))

  "When the user inputs the data that is posted in the form, the role within business" must {

    "successfully pass validation for Beneficial Shareholder" in {
      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("01"))
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Valid(BeneficialShareholder))
    }

    "successfully pass validation for Director" in {
      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("02"))
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Valid(Director))
    }

    "successfully pass validation for External Accountant" in {
      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("03"))
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Valid(ExternalAccountant))
    }

    "successfully pass validation for Internal Accountant" in {
      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("04"))
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Valid(InternalAccountant))
    }

    "successfully pass validation for Nominated Officer" in {
      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("05"))
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Valid(NominatedOfficer))
    }

    "successfully pass validation for Partner" in {
      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("06"))
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Valid(Partner))
    }

    "successfully pass validation for Sole Proprietor" in {
      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("07"))
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Valid(SoleProprietor))
    }

    "successfully pass validation for Other value" in {
      val urlFormEncoded = Map(
        "roleWithinBusiness" -> Seq("08"),
        "roleWithinBusinessOther" -> Seq("Some other value")
      )
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Valid(Other("Some other value")))
    }

    "fail validation if not Other value" in {
      val urlFormEncoded = Map(
        "roleWithinBusiness" -> Seq("08"),
        "roleWithinBusinessOther" -> Seq("")
      )
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Invalid(Seq(
        (Path \ "roleWithinBusinessOther") -> Seq(ValidationError("error.required.declaration.specify.role"))
      )))
    }

    "fail to validate given an invalid value supplied that is not matching to any role" in {

      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("10"))

      RoleWithinBusiness.formRule.validate(urlFormEncoded) must
        be(Invalid(Seq(
          (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.invalid"))
        )))
    }
  }

  "otherDetailsType" must {

    "pass validation" when {
      "255 valid characters are entered" in {
        RoleWithinBusiness.otherDetailsType.validate("1" * 255) must be(Valid("1" * 255))
      }
    }

    "fail validation" when {
      "more than 255 characters are entered" in {
        RoleWithinBusiness.otherDetailsType.validate("1" * 256) must be(
          Invalid(Seq(Path -> Seq(ValidationError("error.invalid.maxlength.255")))))
      }
      "given an empty string" in {
        RoleWithinBusiness.otherDetailsType.validate("") must be(
          Invalid(Seq(Path -> Seq(ValidationError("error.required.declaration.specify.role")))))
      }
      "given whitespace only" in {
        RoleWithinBusiness.otherDetailsType.validate("     ") must be(
          Invalid(Seq(Path -> Seq(ValidationError("error.required.declaration.specify.role")))))
      }
      "given invalid characters" in {
        RoleWithinBusiness.otherDetailsType.validate("{}") must be(
          Invalid(Seq(Path -> Seq(ValidationError("err.text.validation")))))
      }
    }
  }

  "When the user loads the page and that is posted to the form, the role within business" must {

    "load the correct value in the form for Beneficial Shareholder" in {
      RoleWithinBusiness.formWrite.writes(BeneficialShareholder) must be(Map("roleWithinBusiness" -> Seq("01")))
    }

    "load the correct value in the form for Director" in {
      RoleWithinBusiness.formWrite.writes(Director) must be(Map("roleWithinBusiness" -> Seq("02")))
    }

    "load the correct value in the form for ExternalAccountant" in {
      RoleWithinBusiness.formWrite.writes(ExternalAccountant) must be(Map("roleWithinBusiness" -> Seq("03")))
    }

    "load the correct value in the form for InternalAccountant" in {
      RoleWithinBusiness.formWrite.writes(InternalAccountant) must be(Map("roleWithinBusiness" -> Seq("04")))
    }

    "load the correct value in the form for NominatedOfficer" in {
      RoleWithinBusiness.formWrite.writes(NominatedOfficer) must be(Map("roleWithinBusiness" -> Seq("05")))
    }

    "load the correct value in the form for Partner" in {
      RoleWithinBusiness.formWrite.writes(Partner) must be(Map("roleWithinBusiness" -> Seq("06")))
    }

    "load the correct value in the form for SoleProprietor" in {
      RoleWithinBusiness.formWrite.writes(SoleProprietor) must be(Map("roleWithinBusiness" -> Seq("07")))
    }

    "load the correct value in the form for Other value" in {
      RoleWithinBusiness.formWrite.writes(Other("some value")) must be(Map(
        "roleWithinBusiness" -> Seq("08"),
        "roleWithinBusinessOther" -> Seq("some value")
      ))
    }
  }

  "JSON" must {

    "Read the json and return the RoleWithinBusiness domain object successfully for the BeneficialShareholder" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "01"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsSuccess(BeneficialShareholder, JsPath))
    }


    "Read the json and return the RoleWithinBusiness domain object successfully for the Director" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "02"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsSuccess(Director, JsPath))
    }


    "Read the json and return the RoleWithinBusiness domain object successfully for the ExternalAccountant" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "03"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsSuccess(ExternalAccountant, JsPath))
    }

    "Read the json and return the RoleWithinBusiness domain object successfully for the InternalAccountant" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "04"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsSuccess(InternalAccountant, JsPath))
    }

    "Read the json and return the RoleWithinBusiness domain object successfully for the NominatedOfficer" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "05"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsSuccess(NominatedOfficer, JsPath))
    }

    "Read the json and return the RoleWithinBusiness domain object successfully for the Partner" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "06"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsSuccess(Partner, JsPath))
    }


    "Read the json and return the RoleWithinBusiness domain object successfully for the SoleProprietor" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "07"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsSuccess(SoleProprietor, JsPath))
    }


    "Read the json and return the given `other` value" in {

      val json = Json.obj(
        "roleWithinBusiness" -> "08",
        "roleWithinBusinessOther" -> "any other value"
      )

      Json.fromJson[RoleWithinBusiness](json) must
        be(JsSuccess(Other("any other value"), JsPath \ "roleWithinBusinessOther"))
    }

    "Read the json and return error if an invalid value is found" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "10"
      )
      RoleWithinBusiness.jsonReads.reads(json) must be(JsError((JsPath) -> play.api.data.validation.ValidationError("error.invalid")))
    }



    "Write the json successfully from the BeneficialShareholder domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = BeneficialShareholder
      val json = Json.obj(
        "roleWithinBusiness" -> "01"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }


    "Write the json successfully from the Director domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = Director
      val json = Json.obj(
        "roleWithinBusiness" -> "02"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }

    "Write the json successfully from the ExternalAccountant domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = ExternalAccountant
      val json = Json.obj(
        "roleWithinBusiness" -> "03"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }

    "Write the json successfully from the InternalAccountant domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = InternalAccountant
      val json = Json.obj(
        "roleWithinBusiness" -> "04"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }

    "Write the json successfully from the NominatedOfficer domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = NominatedOfficer
      val json = Json.obj(
        "roleWithinBusiness" -> "05"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }

    "Write the json successfully from the Partner domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = Partner
      val json = Json.obj(
        "roleWithinBusiness" -> "06"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }

    "Write the json successfully from the SoleProprietor domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = SoleProprietor
      val json = Json.obj(
        "roleWithinBusiness" -> "07"
      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }

    "Write the json successfully from the Other domain object created" in {

      val roleWithinBusiness: RoleWithinBusiness = Other("any other value")
      val json = Json.obj(
        "roleWithinBusiness" -> "08",
        "roleWithinBusinessOther" -> "any other value"

      )
      RoleWithinBusiness.jsonWrites.writes(roleWithinBusiness) must be(json)
    }


  }


}


