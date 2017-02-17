package models.tradingpremises

import cats.data.Validated.Valid
import models.FormTypes._
import models.DateOfChange
import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.libs.json._
import typeclasses.MongoKey
import config.ApplicationConfig

case class AgentName(agentName: String,
                     dateOfChange: Option[DateOfChange] = None,
                     agentDateOfBirth: Option[LocalDate] = None
                    )

object AgentName {

  import utils.MappingUtils.Implicits._

  def applyWithoutDateOfChange(agentName: String, agentDateOfBirth: Option[LocalDate]) =
    AgentName(agentName, None, agentDateOfBirth)

  val maxAgentNameLength = 140

  private val agentNameType = notEmptyStrip andThen notEmpty.withMessage("error.required.tp.agent.name") andThen
    maxLength(maxAgentNameLength).withMessage("error.invalid.tp.agent.name") andThen
    basicPunctuationPattern

  implicit val mongoKey = new MongoKey[AgentName] {
    override def apply(): String = "agent-name"
  }
  implicit val format = Json.format[AgentName]

  implicit def formReads: Rule[UrlFormEncoded, AgentName] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    ((__ \ "agentName").read(agentNameType) ~
      {ApplicationConfig.release7 match {
      case true => (__ \ "agentDateOfBirth").read(localDateRule).map(x=>Some(x))
      case false => Rule[UrlFormEncoded, Option[LocalDate]](_ => Valid(None))
    }}) (AgentName.applyWithoutDateOfChange _)
  }

  implicit val formWrites: Write[AgentName, UrlFormEncoded] = Write {
    case AgentName(crn, _, agentDateOfBirth: Option[LocalDate]) => Map("agentName" -> Seq(crn)) ++ {
      agentDateOfBirth match {
        case Some(dob) => localDateWrite.writes(dob) map {
          case (key, value) =>
            s"agentDateOfBirth.$key" -> value
        }
        case _ => Nil
      }
    }

  }

  implicit def convert(data: AgentName): Option[TradingPremises] = {
    Some(TradingPremises(agentName = Some(data)))
  }

}
