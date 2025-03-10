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
import models.tradingpremises.AgentName
import java.time.LocalDate
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class AgentNameFormProvider @Inject() () extends Mappings {

  val length                   = 140
  def apply(): Form[AgentName] = Form[AgentName](
    mapping(
      "agentName"        -> text("error.required.tp.agent.name").verifying(
        firstError(
          maxLength(length, "error.length.tp.agent.name"),
          regexp(basicPunctuationRegex, "error.char.tp.agent.name")
        )
      ),
      "agentDateOfBirth" -> localDate(
        oneInvalidKey = "error.invalid.tp.agent.date.one",
        multipleInvalidKey = "error.invalid.tp.agent.date.multiple",
        oneRequiredKey = "error.required.tp.agent.date.one",
        twoRequiredKey = "error.required.tp.agent.date.two",
        allRequiredKey = "error.required.tp.agent.date.all",
        realDateKey = "error.invalid.agent.date.not.real"
      ).verifying(
        minDate(LocalDate.of(1900, 1, 1), "error.invalid.agent.date.1900"),
        maxDate(LocalDate.now(), "error.invalid.agent.date.future")
      )
    )((name, date) => AgentName(name, agentDateOfBirth = Some(date)))(x =>
      x.agentDateOfBirth.map(y => (x.agentName, y))
    )
  )

}
