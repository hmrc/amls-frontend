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

case class AgentCompanyName(agentCompanyName: String)

object AgentCompanyName {

  import utils.MappingUtils.Implicits._

  val maxAgentRegisteredCompanyNameLength = 140

  val agentsRegisteredCompanyNameType =  notEmptyStrip andThen notEmpty.withMessage("error.required.tp.agent.registered.company.name") andThen
    maxLength(maxAgentRegisteredCompanyNameLength).withMessage("error.invalid.tp.agent.registered.company.name")

  implicit val mongoKey = new MongoKey[AgentCompanyName] {
    override def apply(): String = "agent-company-name"
  }
  implicit val format = Json.format[AgentCompanyName]

  implicit val formReads: Rule[UrlFormEncoded, AgentCompanyName] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "agentCompanyName").read(agentsRegisteredCompanyNameType) map AgentCompanyName.apply
  }

  implicit val formWrites: Write[AgentCompanyName, UrlFormEncoded] = Write {
    case AgentCompanyName(crn) => Map("agentCompanyName" -> Seq(crn))
  }
}