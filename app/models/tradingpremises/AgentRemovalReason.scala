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

package models.tradingpremises

import models.tradingpremises.AgentRemovalReason.{CeasedTrading, LackOfProfit, MajorComplianceIssues, MinorComplianceIssues, Other, RequestedByAgent}
import models.tradingpremises.RemovalReasonConstants.Schema
import models.{Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

case class AgentRemovalReason(removalReason: String, removalReasonOther: Option[String] = None) {

  def reasonToObj: AgentRemovalReasonAnswer = removalReason match {
    case Schema.MAJOR_COMPLIANCE_ISSUES => MajorComplianceIssues
    case Schema.MINOR_COMPLIANCE_ISSUES => MinorComplianceIssues
    case Schema.LACK_OF_PROFIT          => LackOfProfit
    case Schema.CEASED_TRADING          => CeasedTrading
    case Schema.REQUESTED_BY_AGENT      => RequestedByAgent
    case Schema.OTHER                   => Other
    case _                              => throw new IllegalArgumentException("Invalid reason")
  }
}

sealed trait AgentRemovalReasonAnswer {
  val value: String
}
object AgentRemovalReason extends Enumerable.Implicits {

  case object MajorComplianceIssues extends WithName("majorComplianceIssues") with AgentRemovalReasonAnswer {
    val value = "01"
  }
  case object MinorComplianceIssues extends WithName("minorComplianceIssues") with AgentRemovalReasonAnswer {
    val value = "02"
  }
  case object LackOfProfit extends WithName("lackOfProfit") with AgentRemovalReasonAnswer {
    val value = "03"
  }
  case object CeasedTrading extends WithName("ceasedTrading") with AgentRemovalReasonAnswer {
    val value = "04"
  }
  case object RequestedByAgent extends WithName("requestedByAgent") with AgentRemovalReasonAnswer {
    val value = "05"
  }
  case object Other extends WithName("other") with AgentRemovalReasonAnswer {
    val value = "06"
  }

  val all: Seq[AgentRemovalReasonAnswer] = Seq(
    MajorComplianceIssues,
    MinorComplianceIssues,
    LackOfProfit,
    CeasedTrading,
    RequestedByAgent,
    Other
  )

  implicit val enumerable: Enumerable[AgentRemovalReasonAnswer] = Enumerable(all.map(v => v.toString -> v): _*)

  def formItems(conditionalHtml: Html)(implicit messages: Messages): Seq[RadioItem] = all.map { answer =>
    if (answer == Other) {
      RadioItem(
        content = Text(messages(s"tradingpremises.remove_reasons.agent.premises.lbl.${answer.value}")),
        id = Some(answer.toString),
        value = Some(answer.toString),
        conditionalHtml = Some(conditionalHtml)
      )
    } else {
      RadioItem(
        content = Text(messages(s"tradingpremises.remove_reasons.agent.premises.lbl.${answer.value}")),
        id = Some(answer.toString),
        value = Some(answer.toString)
      )
    }
  }

  implicit val formats: OFormat[AgentRemovalReason] = Json.format[AgentRemovalReason]
}

object RemovalReasonConstants {

  object Form {
    val MAJOR_COMPLIANCE_ISSUES = "01"
    val MINOR_COMPLIANCE_ISSUES = "02"
    val LACK_OF_PROFIT          = "03"
    val CEASED_TRADING          = "04"
    val REQUESTED_BY_AGENT      = "05"
    val OTHER                   = "06"
  }

  object Schema {
    val MAJOR_COMPLIANCE_ISSUES = "Serious compliance failures"
    val MINOR_COMPLIANCE_ISSUES = "Minor compliance failures"
    val LACK_OF_PROFIT          = "Lack of activity"
    val CEASED_TRADING          = "Agent ceased trading"
    val REQUESTED_BY_AGENT      = "Requested by agent"
    val OTHER                   = "Other"
  }

  object Rules {

    def toSchemaReason(reason: String): String =
      reason match {
        case Form.MAJOR_COMPLIANCE_ISSUES => Schema.MAJOR_COMPLIANCE_ISSUES
        case Form.MINOR_COMPLIANCE_ISSUES => Schema.MINOR_COMPLIANCE_ISSUES
        case Form.LACK_OF_PROFIT          => Schema.LACK_OF_PROFIT
        case Form.CEASED_TRADING          => Schema.CEASED_TRADING
        case Form.REQUESTED_BY_AGENT      => Schema.REQUESTED_BY_AGENT
        case Form.OTHER                   => Schema.OTHER
      }

    def fromSchemaReason(reason: String): String =
      reason match {
        case Schema.MAJOR_COMPLIANCE_ISSUES => Form.MAJOR_COMPLIANCE_ISSUES
        case Schema.MINOR_COMPLIANCE_ISSUES => Form.MINOR_COMPLIANCE_ISSUES
        case Schema.LACK_OF_PROFIT          => Form.LACK_OF_PROFIT
        case Schema.CEASED_TRADING          => Form.CEASED_TRADING
        case Schema.REQUESTED_BY_AGENT      => Form.REQUESTED_BY_AGENT
        case Schema.OTHER                   => Form.OTHER
      }

  }

}
