/*
 * Copyright 2023 HM Revenue & Customs
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

package models.tradingpremises

import jto.validation.{Invalid, Path, Valid, ValidationError}
import play.api.libs.json.{JsSuccess, Json}
import utils.AmlsSpec

class AgentCompanyDetailsSpec extends AmlsSpec {

  "AgentCompanyDetails" must {

    "validate form Read" in {
      val formInput = Map("agentCompanyName" -> Seq("sometext"), "companyRegistrationNumber" -> Seq("12345678"))
      AgentCompanyDetails.formReads.validate(formInput) must be(Valid(AgentCompanyDetails("sometext", Some("12345678"))))
    }

    "throw error" when {

      "name field is missing" in {
        val formInput = Map("agentCompanyName" -> Seq(""), "companyRegistrationNumber" -> Seq("12345678"))
        AgentCompanyDetails.formReads.validate(formInput) must be(Invalid(Seq((
          Path \ "agentCompanyName",
          Seq(ValidationError("error.required.tp.agent.company.details"))
        ))))
      }

      "crn field is missing" in {
        val formInput = Map("agentCompanyName" -> Seq("sometext"), "companyRegistrationNumber" -> Seq(""))
        AgentCompanyDetails.formReads.validate(formInput) must be(Invalid(Seq((
          Path \ "companyRegistrationNumber",
          Seq(ValidationError("error.required.to.agent.company.reg.number"))
        ))))
      }

      "given a value with length greater than 8" in {
        val data = Map("agentCompanyName" -> Seq("sometext"), "companyRegistrationNumber" -> Seq("1" * 9))
        val result = AgentCompanyDetails.formReads.validate(data)
        result mustBe Invalid(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.size.to.agent.company.reg.number"))))
      }

      "given a value with length less than 8" in {
        val data = Map("agentCompanyName" -> Seq("sometext"), "companyRegistrationNumber" -> Seq("1" * 7))
        val result = AgentCompanyDetails.formReads.validate(data)
        result mustBe Invalid(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.size.to.agent.company.reg.number"))))
      }

      "given a value containing non-alphanumeric characters" in {
        val data = Map("agentCompanyName" -> Seq("sometext"), "companyRegistrationNumber" -> Seq("1234567!"))
        val result = AgentCompanyDetails.formReads.validate(data)
        result mustBe Invalid(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.char.to.agent.company.reg.number"))))
      }

      "both fields missing" in {
        val formInput = Map("agentCompanyName" -> Seq(""), "companyRegistrationNumber" -> Seq(""))
        AgentCompanyDetails.formReads.validate(formInput) must be(Invalid(Seq((
          Path \ "agentCompanyName",
          Seq(ValidationError("error.required.tp.agent.company.details"))
        ), (
          Path \ "companyRegistrationNumber",
          Seq(ValidationError("error.required.to.agent.company.reg.number"))
        )
        )))
      }

      "input exceeds max length" in {
        val formInput = Map("agentCompanyName" -> Seq("sometesttexttest" * 11), "companyRegistrationNumber" -> Seq("12345678"))
        AgentCompanyDetails.formReads.validate(formInput) must be(Invalid(Seq((
          Path \ "agentCompanyName") -> Seq(ValidationError("error.invalid.tp.agent.company.details")
        ))))
      }

      "input has invalid data" in {
        val formInput = Map("agentCompanyName" -> Seq("<sometest>"), "companyRegistrationNumber" -> Seq("12345678"))
        AgentCompanyDetails.formReads.validate(formInput) must be(Invalid(Seq((
          Path \ "agentCompanyName") -> Seq(ValidationError("error.invalid.char.tp.agent.company.details")
        ))))
      }

    }

    "validate form write" in {
      AgentCompanyDetails.formWrites.writes(AgentCompanyDetails("sometext", Some("12345678"))) must be(
        Map("agentCompanyName" -> Seq("sometext"), "companyRegistrationNumber" -> Seq("12345678")))
    }


  }

  "Json Validation" must {
    "Successfully read/write Json data" in {
      Json.fromJson[AgentCompanyDetails](Json.toJson[AgentCompanyDetails](
        AgentCompanyDetails("test", "12345678"))) must be(JsSuccess(AgentCompanyDetails("test", Some("12345678"))))
    }
  }
}
