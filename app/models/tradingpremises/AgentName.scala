/*
 * Copyright 2019 HM Revenue & Customs
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

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import models.DateOfChange
import models.FormTypes._
import org.joda.time.LocalDate
import play.api.libs.json._
import typeclasses.MongoKey

case class AgentName(agentName: String,
                     dateOfChange: Option[DateOfChange] = None,
                     agentDateOfBirth: Option[LocalDate] = None
                    )

object AgentName {

  import utils.MappingUtils.Implicits._

  val dateRule = newAllowedPastAndFutureDateRule("error.required.tp.agent.date", "error.char.tp.agent.dob", "error.required.tp.agent.date.past")
  def applyWithoutDateOfChange(agentName: String, agentDateOfBirth: Option[LocalDate]) =
    AgentName(agentName, None, agentDateOfBirth)

  val maxAgentNameLength = 140

  private val agentNameType = notEmptyStrip andThen notEmpty.withMessage("error.required.tp.agent.name") andThen
    maxLength(maxAgentNameLength).withMessage("error.length.tp.agent.name") andThen
    regexWithMsg(basicPunctuationRegex, "error.char.tp.agent.name")

  implicit val mongoKey = new MongoKey[AgentName] {
    override def apply(): String = "agent-name"
  }
  implicit val format = Json.format[AgentName]

  implicit def formReads: Rule[UrlFormEncoded, AgentName] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (
      (__ \ "agentName").read(agentNameType) ~
      (__ \ "agentDateOfBirth").read(dateRule).map(x=>Some(x))
    ) (applyWithoutDateOfChange)
  }

  implicit val formWrites: Write[AgentName, UrlFormEncoded] = Write {
    case AgentName(crn, _, agentDateOfBirth: Option[LocalDate]) => Map("agentName" -> Seq(crn)) ++ {
      agentDateOfBirth match {
        case Some(dob) => localDateWrite.writes(dob) map {
          case (key, value) =>
            s"agentDateOfBirth.$key" -> value
        }
        case _ => Nil
      }
    }

  }

  implicit def convert(data: AgentName): Option[TradingPremises] = {
    Some(TradingPremises(agentName = Some(data)))
  }

}
