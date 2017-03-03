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
