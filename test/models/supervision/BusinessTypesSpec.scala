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

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import utils.GenericTestHelper

class BusinessTypesSpec extends PlaySpec with GenericTestHelper {

  "Form validation" must {

    "return success" when {
      "more than one check box is selected" in {

        val model = Map(
          "businessType[]" -> Seq("01", "02", "04","05","06","07","08","09","10","11","12","13","14"),
          "specifyOtherBusiness" -> Seq("test")
        )

        ProfessionalBodies.formRule.validate(model) must
          be(Valid(ProfessionalBodies(Set(AccountingTechnicians, CharteredCertifiedAccountants, TaxationTechnicians, ManagementAccountants,
            InstituteOfTaxation, Bookkeepers, AccountantsIreland, AccountantsScotland, AccountantsEnglandandWales, FinancialAccountants,
            AssociationOfBookkeepers, LawSociety, Other("test")
          ))))

      }
    }

    "return error" when {

      "'Other' is selected, but specifyOtherBusiness is an empty string" in {

        val model = Map(
          "businessType[]" -> Seq("01", "02", "14"),
          "specifyOtherBusiness" -> Seq("")
        )

        ProfessionalBodies.formRule.validate(model) must
          be(Invalid(List((Path \ "specifyOtherBusiness", Seq(ValidationError("error.required.supervision.business.details"))))))
      }

      "specifyOtherBusiness exceeds max length" in {

        val model = Map(
          "businessType[]" -> Seq("01", "02", "14"),
          "specifyOtherBusiness" -> Seq("test" * 200)
        )

        ProfessionalBodies.formRule.validate(model) must
          be(Invalid(List((Path \ "specifyOtherBusiness", Seq(ValidationError("error.invalid.supervision.business.details"))))))
      }

      "businessType[] is empty" in {

        val model = Map(
          "businessType[]" -> Seq()
        )

        ProfessionalBodies.formRule.validate(model) must
          be(Invalid(List((Path \ "businessType", Seq(ValidationError("error.required.supervision.one.professional.body"))))))
      }

      "given no data represented by an empty Map" in {

        ProfessionalBodies.formRule.validate(Map.empty) must
          be(Invalid(Seq((Path \ "businessType") -> Seq(ValidationError("error.required.supervision.one.professional.body")))))

      }

      "given invalid businessType[] selection" in {

        val model = Map(
          "businessType[]" -> Seq("01", "20")
        )

        ProfessionalBodies.formRule.validate(model) must
          be(Invalid(Seq((Path \ "businessType") -> Seq(ValidationError("error.invalid")))))

      }

      "given invalid characters in specifyOther" in {

        val model = Map(
          "businessType[]" -> Seq("14"),
          "specifyOtherBusiness" -> Seq("{}{}")
        )

        ProfessionalBodies.formRule.validate(model) must
          be(Invalid(Seq((Path \ "specifyOtherBusiness", Seq(ValidationError("err.text.validation"))))))
      }

    }

  }

  "Form writers" must {
    "write to form with businessTypes" when {
      "without other option" in {

        val map = Map(
          "businessType[]" -> Seq("03","04", "05", "06")
        )

        val model = ProfessionalBodies(Set(InternationalAccountants, TaxationTechnicians, ManagementAccountants, InstituteOfTaxation))

        ProfessionalBodies.formWrites.writes(model) must be (map)
      }

      "with Other option" in {

        val map = Map(
          "businessType[]" -> Seq("14","13"),
          "specifyOtherBusiness" -> Seq("otherBusiness")
        )

        val model = ProfessionalBodies(Set(Other("otherBusiness"), LawSociety))
        ProfessionalBodies.formWrites.writes(model) must be (map)
      }
    }
  }

  "JSON validation" must {

    "validate given values" in {
      val json =  Json.obj("businessType" -> Seq("01","02"))

      Json.fromJson[ProfessionalBodies](json) must
        be(JsSuccess(ProfessionalBodies(Set(AccountingTechnicians, CharteredCertifiedAccountants)), JsPath))
    }

    "validate given values with option Digital software" in {
      val json =  Json.obj(
        "businessType" -> Seq("14", "12"),
        "specifyOtherBusiness" -> "test"
      )

      Json.fromJson[ProfessionalBodies](json) must
        be(JsSuccess(ProfessionalBodies(Set(Other("test"), AssociationOfBookkeepers)), JsPath ))
    }

    "fail when on path is missing" in {
      Json.fromJson[ProfessionalBodies](Json.obj("isAMember" -> true)) must
        be(JsError((JsPath \"businessType") -> play.api.data.validation.ValidationError("error.path.missing")))
    }

    "fail when on invalid data" in {
      Json.fromJson[ProfessionalBodies](Json.obj("businessType" -> Seq("40"))) must
        be(JsError((JsPath \ "businessType") -> play.api.data.validation.ValidationError("error.invalid")))
    }

  }

  "JSON writers" must {
    "write valid data" in {
      Json.toJson[ProfessionalBodies](ProfessionalBodies(Set(AccountantsScotland, Other("test657")))) must
        be (Json.obj("businessType" -> Seq("09", "14"), "specifyOtherBusiness" -> "test657"))
    }
  }

}
