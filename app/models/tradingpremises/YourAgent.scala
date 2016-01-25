package models.tradingpremises

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

case class YourAgent (agentsRegisteredName: AgentsRegisteredName,
                      taxType: TaxType,
                      businessStructure: BusinessStructure
)

case class AgentsRegisteredName(value : String)

sealed trait TaxType
case object TaxTypeSelfAssesment extends TaxType
case object TaxTypeCorporationTax extends TaxType

sealed trait BusinessStructure

case object SoleProprietor extends BusinessStructure

case object LimitedLiabilityPartnership extends BusinessStructure

case object Partnership extends BusinessStructure

case object IncorporatedBody extends BusinessStructure

case object UnincorporatedBody extends BusinessStructure


object YourAgent {

  val key = "your-agent"
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, YourAgent] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._

    (__.read[AgentsRegisteredName] and
      __.read[TaxType] and
      __.read[BusinessStructure]).apply(YourAgent.apply _)
  }

  implicit val agentsRegisteredNameRule : Rule[UrlFormEncoded, AgentsRegisteredName] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._

    (__ \ "agentRegisteredName").read[String] fmap (AgentsRegisteredName.apply)
  }

  implicit val taxTypeRule : Rule[UrlFormEncoded, TaxType] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._

    (__ \ "taxType").read[String] flatMap {
      case "01" => TaxTypeSelfAssesment
      case "02" => TaxTypeCorporationTax
      case _ =>
        (Path \ "yourAgent") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val agentsBusinessStructureRule : Rule[UrlFormEncoded, BusinessStructure] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._

    (__ \ "agentsBusinessStructure").read[String] flatMap {
      case "01" => SoleProprietor
      case "02" => LimitedLiabilityPartnership
      case "03" => Partnership
      case "04" => IncorporatedBody
      case "05" => UnincorporatedBody
      case _ =>
        (Path \ "yourAgent") -> Seq(ValidationError("error.invalid"))
    }
  }



  implicit val formWrites: Write[YourAgent, UrlFormEncoded] = Write {
    case a: YourAgent =>
      Map("agentRegisteredName" -> Seq("xyz"))
    case _ => Map("previouslyRegistered" -> Seq("false"))
  }

  implicit val jsonReads = {
    (__ \ "taxType").read[String].flatMap[TaxType] {
        case "01" => TaxTypeSelfAssesment
        case "02" => TaxTypeCorporationTax
        case _ => ValidationError("error.invalid")
      }
  }

  implicit val jsonWrites = Writes[YourAgent] {
    Json.obj(
      "agentRegisteredName" -> "xyz"
    )
  }
}