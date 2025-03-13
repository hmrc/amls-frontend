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

package utils.deregister

import models.deregister.DeregistrationReason
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{Key, SummaryListRow, Text, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, SummaryList}

object CheckYourAnswersHelper {

  def createSummaryList(deregistrationReason: DeregistrationReason)(implicit messages: Messages): SummaryList =
    SummaryList(rows =
      Seq(
        SummaryListRow(
          Key(Text(messages("deregistration.cya.question"))),
          Value(Text(messages(s"deregistration.reason.lbl.${deregistrationReason.value}"))),
          actions = Some(
            Actions(
              items = Seq(
                ActionItem(
                  href = controllers.deregister.routes.DeregistrationReasonController.get.url,
                  content = Text(messages("button.change")),
                  attributes = Map("id" -> "cya-change-link")
                )
              )
            )
          )
        )
      )
    )
}
