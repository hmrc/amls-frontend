package models.tradingpremises

import models.FormTypes._
import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._
import typeclasses.MongoKey

case class AgentCompanyDetails(agentCompanyName: String, companyRegistrationNumber: String)

object AgentCompanyDetails {

  import utils.MappingUtils.Implicits._

  val maxAgentRegisteredCompanyNameLength = 140
  val agentsRegisteredCompanyNameType: Rule[String, String] =
    notEmptyStrip compose notEmpty.withMessage("error.required.tp.agent.registered.company.name") compose
      maxLength(maxAgentRegisteredCompanyNameLength).withMessage("error.invalid.tp.agent.registered.company.name")

  val agentsRegisteredCompanyCRNType: Rule[String, String] =
    notEmpty.withMessage("error.required.bm.registration.number") compose
    pattern("^[A-Z0-9]{8}$".r).withMessage("error.invalid.bm.registration.number")

  implicit val mongoKey = new MongoKey[AgentCompanyDetails] {
    override def apply(): String = "agent-company-name"
  }
  implicit val format = Json.format[AgentCompanyDetails]

  implicit val formReads: Rule[UrlFormEncoded, AgentCompanyDetails] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (
      (__ \ "agentCompanyName").read(agentsRegisteredCompanyNameType) ~
        (__ \ "companyRegistrationNumber").read(agentsRegisteredCompanyCRNType)
      ) (AgentCompanyDetails(_, _))
  }

  implicit val formWrites: Write[AgentCompanyDetails, UrlFormEncoded] = Write {
    case AgentCompanyDetails(name, crn) => Map("agentCompanyName" -> Seq(name), "companyRegistrationNumber" -> Seq(crn))
  }
}
