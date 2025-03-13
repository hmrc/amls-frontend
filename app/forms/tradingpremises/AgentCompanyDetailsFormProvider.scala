/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.tradingpremises

import forms.mappings.Mappings
import models.tradingpremises.AgentCompanyDetails
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class AgentCompanyDetailsFormProvider @Inject() () extends Mappings {

  val companyNameLength   = 140
  val companyNumberLength = 8

  val crnNumberRegex                     = "^[A-Z0-9]{8}$"
  def apply(): Form[AgentCompanyDetails] = Form[AgentCompanyDetails](
    mapping(
      "agentCompanyName"          -> text("error.required.tp.agent.company.details").verifying(
        firstError(
          maxLength(companyNameLength, "error.invalid.tp.agent.company.details"),
          regexp(basicPunctuationRegex, "error.invalid.char.tp.agent.company.details")
        )
      ),
      "companyRegistrationNumber" -> text("error.required.to.agent.company.reg.number").verifying(
        firstError(
          minLength(companyNumberLength, "error.size.to.agent.company.reg.number"),
          maxLength(companyNumberLength, "error.size.to.agent.company.reg.number"),
          regexp(crnNumberRegex, "error.char.to.agent.company.reg.number")
        )
      )
    )(AgentCompanyDetails.apply)(x => x.companyRegistrationNumber.map(y => (x.agentCompanyName, y)))
  )
}
