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

package models.tradingpremises

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}

class AgentCompanyNameSpec extends PlaySpec {

  "AgentCompanyName" must {

    "validate form Read" in {
      val formInput = Map("agentCompanyName" -> Seq("sometext"))
      AgentCompanyName.formReads.validate(formInput) must be(Valid(AgentCompanyName("sometext")))
    }

    "throw error when required field is missing" in {
      val formInput = Map("agentCompanyName" -> Seq(""))
      AgentCompanyName.formReads.validate(formInput) must be(Invalid(Seq((Path \ "agentCompanyName", Seq(ValidationError("error.required.tp.agent.registered.company.name"))))))
    }

    "throw error when input exceeds max length" in {
      val formInput = Map("agentCompanyName" -> Seq("sometesttexttest"*11))
      AgentCompanyName.formReads.validate(formInput) must be(Invalid(Seq((Path \ "agentCompanyName") -> Seq(ValidationError("error.invalid.tp.agent.registered.company.name")))))
    }

    "validate form write" in {
      AgentCompanyName.formWrites.writes(AgentCompanyName("sometext")) must be(Map("agentCompanyName" -> Seq("sometext")))
    }


  }

  "Json Validation" must {
    "Successfully read/write Json data" in {
      AgentCompanyName.format.reads(AgentCompanyName.format.writes(
        AgentCompanyName("test"))) must be(JsSuccess(AgentCompanyName("test"), JsPath \ "agentCompanyName"))
    }
  }
}