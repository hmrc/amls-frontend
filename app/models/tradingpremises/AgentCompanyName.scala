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

case class AgentCompanyName(agentCompanyName: String)

object AgentCompanyName {

  import utils.MappingUtils.Implicits._

  val maxAgentRegisteredCompanyNameLength = 140

  val agentsRegisteredCompanyNameType =  notEmptyStrip compose notEmpty.withMessage("error.required.tp.agent.registered.company.name") compose
  maxLength(maxAgentRegisteredCompanyNameLength).withMessage("error.invalid.tp.agent.registered.company.name")

  implicit val mongoKey = new MongoKey[AgentCompanyName] {
    override def apply(): String = "agent-company-name"
  }
  implicit val format = Json.format[AgentCompanyName]

  implicit val formReads: Rule[UrlFormEncoded, AgentCompanyName] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "agentCompanyName").read(agentsRegisteredCompanyNameType) fmap AgentCompanyName.apply
  }

  implicit val formWrites: Write[AgentCompanyName, UrlFormEncoded] = Write {
    case AgentCompanyName(crn) => Map("agentCompanyName" -> Seq(crn))
  }

}