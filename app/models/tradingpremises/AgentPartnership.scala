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

import models.FormTypes._
import jto.validation._
import jto.validation.forms.Rules._
import play.api.libs.json._
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._
import typeclasses.MongoKey
import utils.{JsonMapping, TraversableValidators}

case class AgentPartnership(agentPartnership: String)

object AgentPartnership {

  import utils.MappingUtils.Implicits._

  val maxAgentPartnershipLength = 140

  private val agentsPartnershipType =  notEmptyStrip andThen notEmpty.withMessage("error.required.tp.agent.partnership") andThen
    maxLength(maxAgentPartnershipLength).withMessage("error.invalid.tp.agent.partnership") andThen
    basicPunctuationPattern()

  implicit val mongoKey = new MongoKey[AgentPartnership] {
    override def apply(): String = "agent-partnership"
  }
  implicit val format = Json.format[AgentPartnership]

  implicit val formReads: Rule[UrlFormEncoded, AgentPartnership] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "agentPartnership").read(agentsPartnershipType) map AgentPartnership.apply
  }

  implicit val formWrites: Write[AgentPartnership, UrlFormEncoded] = Write {
    case AgentPartnership(ap) => Map("agentPartnership" -> Seq(ap))
  }
}
