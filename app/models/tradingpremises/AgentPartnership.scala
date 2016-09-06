package models.tradingpremises

import models.FormTypes._
import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.libs.json._
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._
import typeclasses.MongoKey
import utils.{JsonMapping, TraversableValidators}

case class AgentPartnership(agentPartnership: String)

object AgentPartnership {

  import utils.MappingUtils.Implicits._

  val maxAgentPartnershipLength = 140

  val agentsPartnershipType =  notEmptyStrip compose notEmpty.withMessage("error.required.tp.agent.partnership") compose
    maxLength(maxAgentPartnershipLength).withMessage("error.invalid.tp.agent.partnership")

  implicit val mongoKey = new MongoKey[AgentPartnership] {
    override def apply(): String = "agent-partnership"
  }
  implicit val format = Json.format[AgentPartnership]

  implicit val formReads: Rule[UrlFormEncoded, AgentPartnership] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "agentPartnership").read(agentsPartnershipType) fmap AgentPartnership.apply
  }

  implicit val formWrites: Write[AgentPartnership, UrlFormEncoded] = Write {
    case AgentPartnership(ap) => Map("agentPartnership" -> Seq(ap))
  }

  implicit def convert(data: AgentPartnership): Option[TradingPremises] = {
    Some(TradingPremises(agentPartnership = Some(data)))
  }

}
