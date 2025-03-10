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
import models.tradingpremises.AgentRemovalReason.Other
import models.tradingpremises.RemovalReasonConstants.Rules
import models.tradingpremises.{AgentRemovalReason, AgentRemovalReasonAnswer}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings._

import javax.inject.Inject

class RemoveAgentPremisesReasonsFormProvider @Inject() () extends Mappings {

  val radioErrorMessage                 = "tradingpremises.remove_reasons.missing"
  val length                            = 255
  def apply(): Form[AgentRemovalReason] = Form[AgentRemovalReason](
    mapping(
      "removalReason"      -> enumerable[AgentRemovalReasonAnswer](radioErrorMessage, radioErrorMessage)(
        AgentRemovalReason.enumerable
      ),
      "removalReasonOther" -> mandatoryIf(
        _.get("removalReason").contains(Other.toString),
        text("tradingpremises.remove_reasons.agent.other.missing").verifying(
          firstError(
            maxLength(length, "error.invalid.maxlength.255"),
            regexp(basicPunctuationRegex, "err.text.validation")
          )
        )
      )
    )((reason, reasonOther) => AgentRemovalReason(Rules.toSchemaReason(reason.value), reasonOther))(x =>
      Some((x.reasonToObj, x.removalReasonOther))
    )
  )
}
