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

import cats.data.Validated.Valid
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, To, Write}
import models.FormTypes._
import play.api.libs.json.Json

case class AgentRemovalReason(removalReason: String, removalReasonOther: Option[String] = None)

object AgentRemovalReason {

  import RemovalReasonConstants._
  import utils.MappingUtils.Implicits._
  import models.FormTypes._

  implicit val formats = Json.format[AgentRemovalReason]

  private val otherDetailsLength = 255


  private val otherDetailsRule = notEmptyStrip andThen
    notEmpty.withMessage("tradingpremises.remove_reasons.agent.other.missing") andThen maxLength(otherDetailsLength).
    withMessage("error.invalid.maxlength.255") andThen basicPunctuationPattern()

  private def toSchemaReasonR = Rule.fromMapping[String, String] { v => Valid(Rules.toSchemaReason(v)) }

  implicit val formReader: Rule[UrlFormEncoded, AgentRemovalReason] = From[UrlFormEncoded] { __ =>
    (__ \ "removalReason").read[String].withMessage("tradingpremises.remove_reasons.missing") andThen toSchemaReasonR flatMap {
      case reason@Schema.OTHER =>
        (__ \ "removalReasonOther").read(otherDetailsRule) map { o =>
          AgentRemovalReason(reason, Some(o))
        }
      case reason =>
        AgentRemovalReason(reason)
    }
  }

  implicit val formWriter: Write[AgentRemovalReason, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    (
      (__ \ "removalReason").write[String] ~
        (__ \ "removalReasonOther").write[Option[String]]
      ) (x => (Rules.fromSchemaReason(x.removalReason), x.removalReasonOther))
  }

}

object RemovalReasonConstants {

  object Form {
    val MAJOR_COMPLIANCE_ISSUES = "01"
    val MINOR_COMPLIANCE_ISSUES = "02"
    val LACK_OF_PROFIT = "03"
    val CEASED_TRADING = "04"
    val REQUESTED_BY_AGENT = "05"
    val OTHER = "06"
  }

  object Schema {
    val MAJOR_COMPLIANCE_ISSUES = "Serious compliance failures"
    val MINOR_COMPLIANCE_ISSUES = "Minor compliance failures"
    val LACK_OF_PROFIT = "Lack of activity"
    val CEASED_TRADING = "Agent ceased trading"
    val REQUESTED_BY_AGENT = "Requested by agent"
    val OTHER = "Other"
  }

  object Rules {

    def toSchemaReason(reason: String): String = {
      reason match {
        case Form.MAJOR_COMPLIANCE_ISSUES => Schema.MAJOR_COMPLIANCE_ISSUES
        case Form.MINOR_COMPLIANCE_ISSUES => Schema.MINOR_COMPLIANCE_ISSUES
        case Form.LACK_OF_PROFIT => Schema.LACK_OF_PROFIT
        case Form.CEASED_TRADING => Schema.CEASED_TRADING
        case Form.REQUESTED_BY_AGENT => Schema.REQUESTED_BY_AGENT
        case Form.OTHER => Schema.OTHER
      }
    }

    def fromSchemaReason(reason: String): String = {
      reason match {
        case Schema.MAJOR_COMPLIANCE_ISSUES => Form.MAJOR_COMPLIANCE_ISSUES
        case Schema.MINOR_COMPLIANCE_ISSUES => Form.MINOR_COMPLIANCE_ISSUES
        case Schema.LACK_OF_PROFIT => Form.LACK_OF_PROFIT
        case Schema.CEASED_TRADING => Form.CEASED_TRADING
        case Schema.REQUESTED_BY_AGENT => Form.REQUESTED_BY_AGENT
        case Schema.OTHER => Form.OTHER
      }
    }

  }

}
