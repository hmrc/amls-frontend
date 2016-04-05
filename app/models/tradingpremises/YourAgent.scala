package models.tradingpremises

import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._

case class YourAgent(
                     agentsRegisteredName: String,
                     taxType: TaxType,
                     businessStructure: BusinessStructure
                    )
object YourAgent {

  val key = "your-agent"
  import utils.MappingUtils.Implicits._

  val maxAgentNameLength = 140
  val agentNameType = notEmpty.withMessage("error.required.tp.registered.business.name") compose
    maxLength(maxAgentNameLength).withMessage("error.invalid.tp.registered.business.name")


  implicit val formRule: Rule[UrlFormEncoded, YourAgent] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._

    ((__ \ "agentsRegisteredName").read(agentNameType) ~
      __.read[TaxType] ~
      __.read[BusinessStructure]).apply(YourAgent.apply _)
  }

  implicit val formWrites: Write[YourAgent, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    ((__ \ "agentsRegisteredName").write[String] ~
      __.write[TaxType] ~
      __.write[BusinessStructure]) (unlift(YourAgent.unapply _))
  }

  implicit val jsonReads: Reads[YourAgent] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    ((__ \ "agentsRegisteredName").read[String] and
      __.read[TaxType] and
      __.read[BusinessStructure]) (YourAgent.apply _)
  }

  implicit val jsonWrite: Writes[YourAgent] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    ((__ \ "agentsRegisteredName").write[String] and
      __.write[TaxType] and
      __.write[BusinessStructure]) (unlift(YourAgent.unapply))
  }

  implicit def convert(data: YourAgent): Option[TradingPremises] = {
    Some(TradingPremises(None, Some(data), None))
  }

}
