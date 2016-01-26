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

    (__.read[AgentsRegisteredName] ~
      __.read[TaxType] ~
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
        (Path \ "taxType") -> Seq(ValidationError("error.invalid"))
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
      case _ => (Path \ "agentsBusinessStructure") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[YourAgent, UrlFormEncoded] = To[UrlFormEncoded] {__ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    import models.FormTypes._
    (__.write[AgentsRegisteredName] ~
      __.write[TaxType] ~
      __.write[BusinessStructure])(unlift(YourAgent.unapply _))
  }

  implicit val formWritesRegisteredName: Write[AgentsRegisteredName, UrlFormEncoded] = Write {
    case agentsRegisteredName =>
      Map("agentsRegisteredName" -> Seq(agentsRegisteredName.value))
  }

  implicit val formWritesTaxType : Write[TaxType, UrlFormEncoded] = Write {
    case TaxTypeSelfAssesment =>
      Map("taxType" -> Seq("01"))
    case TaxTypeCorporationTax =>
      Map("taxType" -> Seq("02"))
    case _ => Map("" ->Seq(""))
  }

  implicit val formWritesBusinessStructure: Write[BusinessStructure, UrlFormEncoded] = Write {
    case SoleProprietor =>
      Map("agentsBusinessStructure" -> Seq("01"))
    case LimitedLiabilityPartnership =>
      Map("agentsBusinessStructure" -> Seq("02"))
    case Partnership =>
      Map("agentsBusinessStructure" -> Seq("03"))
    case IncorporatedBody =>
      Map("agentsBusinessStructure" -> Seq("04"))
    case UnincorporatedBody =>
      Map("agentsBusinessStructure" -> Seq("05"))
  }


  implicit val jsonReads: Reads[YourAgent] = {
      import play.api.libs.json._
      import play.api.libs.functional.syntax._
      (__.read[AgentsRegisteredName] and
        __.read[TaxType] and
        __.read[BusinessStructure])(YourAgent.apply _)
  }

  implicit val jsonReadsRegisteredName =  {
    import play.api.libs.json.Reads.StringReads
      (JsPath \ "agentsRegisteredName").read[String] map (AgentsRegisteredName.apply _)
  }

  implicit val jsonReadsTaxType =  {
    (__ \ "taxType").read[String].flatMap[TaxType] {
      case "01" => TaxTypeSelfAssesment
      case "02" => TaxTypeCorporationTax
      case _ => ValidationError("error.invalid")
    }
  }

  implicit val jsonReadsBusinessStructure =  {
    (__ \ "agentsBsinessStructure").read[String].flatMap[BusinessStructure] {
      case "01" => SoleProprietor
      case "02" => LimitedLiabilityPartnership
      case "03" => Partnership
      case "04" => IncorporatedBody
      case "05" => UnincorporatedBody
      case _ =>
        ValidationError("error.invalid")
    }
  }

//  implicit val formats = Json.format[YourAgent]
  implicit val jsonWrite :Writes[YourAgent] = {
    import play.api.libs.json._
    import play.api.libs.functional.syntax._
    (__.write[AgentsRegisteredName] and
      __.write[TaxType] and
      __.write[BusinessStructure])(unlift(YourAgent.unapply))
  }

  implicit val jsonWritesRegisteredName = Writes[AgentsRegisteredName] {
    case agentsRegisteredName => Json.obj("agentsRegisteredName" -> agentsRegisteredName.value)
  }
  implicit val jsonWritesTaxType = Writes[TaxType] {
    case TaxTypeSelfAssesment => Json.obj("taxType" -> "01")
    case TaxTypeCorporationTax => Json.obj("taxType" -> "02")
  }
  implicit val jsonWritesBusinessStructure = Writes[BusinessStructure] {
    case SoleProprietor => Json.obj("agentsBusinessStructure" -> "01")
    case LimitedLiabilityPartnership => Json.obj("agentsBusinessStructure" -> "02")
    case Partnership => Json.obj("agentsBusinessStructure" -> "03")
    case IncorporatedBody => Json.obj("agentsBusinessStructure" -> "04")
    case UnincorporatedBody => Json.obj("agentsBusinessStructure" -> "05")
  }
}