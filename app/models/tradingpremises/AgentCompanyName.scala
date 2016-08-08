package models.tradingpremises.AgentCompanyName
import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._

case class AgentCompanyName(agentsRegisteredCompanyName: String)

object AgentCompanyName {

  import utils.MappingUtils.Implicits._

  val maxAgentRegisteredCompanyNameLength = 140
  val agentsRegisteredCompanyNameType = notEmpty.withMessage("error.required.tp.agent.registered.company.name") compose
    maxLength(maxAgentRegisteredCompanyNameLength).withMessage("error.invalid.tp.agent.registered.company.name")

  implicit val formats = Json.format[AgentCompanyName]

  implicit val formReads: Rule[UrlFormEncoded, AgentCompanyName] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "agentsRegisteredCompanyName").read(agentsRegisteredCompanyNameType) fmap AgentCompanyName.apply
  }

  implicit val formWrites: Write[AgentCompanyName, UrlFormEncoded] = Write {
    case AgentCompanyName(registered) => Map("agentsRegisteredCompanyName" -> Seq(registered))
  }
}