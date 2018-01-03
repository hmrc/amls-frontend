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

package models.tcsp

import org.scalatestplus.play.PlaySpec
import jto.validation.{Path, Invalid, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsSuccess, JsPath, JsError, Json}

class TcspTypesSpec extends PlaySpec {

  "TrustOrCompanyServiceProviders" must {

    val Services = TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider, RegisteredOfficeEtc, CompanyDirectorEtc, CompanyFormationAgent(true, false)))

    "pass validation" when {

      "few checkboxes selected and radio buttons selected for the last checkbox option" in {
        val model = Map(
          "serviceProviders[]" -> Seq("01", "02", "03", "04", "05"),
          "onlyOffTheShelfCompsSold" -> Seq("true"),
          "complexCorpStructureCreation" -> Seq("false")
        )

        TcspTypes.formReads.validate(model) mustBe
          Valid(Services)
      }
    }

    "fail validation" when {

      "return error message when user has not selected any of the services" in {

        TcspTypes.formReads.validate(Map.empty) mustBe
          Invalid(Seq((Path \ "serviceProviders") -> Seq(ValidationError("error.required.tcsp.service.providers"))))
      }

      "return error messages when user hasn't selected the radio buttons for the Trust or company formation agent option" in {
        val model = Map(
          "serviceProviders[]" -> Seq("05"),
          "onlyOffTheShelfCompsSold" -> Seq(""),
          "complexCorpStructureCreation" -> Seq("")
        )

        TcspTypes.formReads.validate(model) mustBe
          Invalid(Seq((Path \ "onlyOffTheShelfCompsSold") -> Seq(ValidationError("error.required.tcsp.off.the.shelf.companies")),
            (Path \ "complexCorpStructureCreation") -> Seq(ValidationError("error.required.tcsp.complex.corporate.structures"))))
      }

      "return failure message when user has filled invalid data" in {

        val model = Map(
          "serviceProviders[]" -> Seq("01", "10")
        )

        TcspTypes.formReads.validate(model) mustBe
          Invalid(Seq((Path \ "serviceProviders") -> Seq(ValidationError("error.invalid"))))
      }

    }

    "form validation" must {

      "write correct data with Trust or company formation agent option selected and associated radio buttons selected" in {
        val model = TcspTypes(Set(RegisteredOfficeEtc, CompanyDirectorEtc, CompanyFormationAgent(true, false)))

        TcspTypes.formWrites.writes(model) mustBe Map("serviceProviders[]" -> Seq("03", "04", "05"),
          "onlyOffTheShelfCompsSold" -> Seq("true"),
          "complexCorpStructureCreation" -> Seq("false"))
      }

      "write correct data with few check boxes selected other than Trust or company formation agent" in {
        val model = TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider))
        TcspTypes.formWrites.writes(model) mustBe Map("serviceProviders[]" -> Seq("01", "02"))
      }
    }

    "Json Validation" must {
      import play.api.data.validation.ValidationError

      "successfully validate given values with option Trust or company formation agent etc" in {
        val json = Json.obj(
          "serviceProviders" -> Seq("01", "02", "03", "04", "05"),
          "onlyOffTheShelfCompsSold" -> true,
          "complexCorpStructureCreation" -> false
        )

        Json.fromJson[TcspTypes](json) must
          be(JsSuccess(Services, JsPath))
      }

      "Read and Write Json valid data successfully" in {

        TcspTypes.jsonReads.reads(Json.toJson(Services))
      }

      "throw error message on reading invalid data" in {

        Json.fromJson[TcspTypes](Json.obj("serviceProviders" -> Seq("40"))) must
          be(JsError((JsPath) \ "serviceProviders" -> ValidationError("error.invalid")))

      }
    }
  }
}



