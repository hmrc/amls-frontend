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
