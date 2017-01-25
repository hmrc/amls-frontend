package models.tradingpremises

import models.FormTypes._
import models.DateOfChange
import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._
import typeclasses.MongoKey

case class AgentName(agentName: String,
                     dateOfChange: Option[DateOfChange] = None
                    )

object AgentName {

  import utils.MappingUtils.Implicits._

  val maxAgentNameLength = 140

  val agentNameType =  notEmptyStrip compose notEmpty.withMessage("error.required.tp.agent.name") compose
    maxLength(maxAgentNameLength).withMessage("error.invalid.tp.agent.name")

  implicit val mongoKey = new MongoKey[AgentName] {
    override def apply(): String = "agent-name"
  }
  implicit val format = Json.format[AgentName]

  implicit val formReads: Rule[UrlFormEncoded, AgentName] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "agentName").read(agentNameType) fmap(AgentName(_))
  }

  implicit val formWrites: Write[AgentName, UrlFormEncoded] = Write {
    case AgentName(crn, _) => Map("agentName" -> Seq(crn))
  }

  implicit def convert(data: AgentName): Option[TradingPremises] = {
    Some(TradingPremises(agentName = Some(data)))
  }

}
