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

import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import play.api.libs.json.Json

case class RegisteringAgentPremises(agentPremises: Boolean)

object RegisteringAgentPremises {

  implicit val formats = Json.format[RegisteringAgentPremises]

  implicit val formRule: Rule[UrlFormEncoded, RegisteringAgentPremises] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      import utils.MappingUtils.Implicits._
      (__ \ "agentPremises").read[Boolean].withMessage("error.required.tp.agent.premises") map RegisteringAgentPremises.apply
    }

  implicit val formWrites: Write[RegisteringAgentPremises, UrlFormEncoded] =
    Write {
      case RegisteringAgentPremises(b) =>
        Map("agentPremises" -> Seq(b.toString))
    }
}
