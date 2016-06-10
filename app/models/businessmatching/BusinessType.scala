package models.businessmatching

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.libs.json._

sealed trait BusinessType

object BusinessType {

  import play.api.data.mapping.forms.Rules._
  import utils.MappingUtils.Implicits._

  case object SoleProprietor extends BusinessType
  case object LimitedCompany extends BusinessType
  case object Partnership extends BusinessType
  case object LPrLLP extends BusinessType
  case object UnincorporatedBody extends BusinessType

  implicit val formR: Rule[UrlFormEncoded, BusinessType] =
    From[UrlFormEncoded] { __ =>
      (__ \ "businessType").read[String] flatMap {
        case "01" => Rule(_ => Success(LimitedCompany))
        case "02" => Rule(_ => Success(SoleProprietor))
        case "03" => Rule(_ => Success(Partnership))
        case "04" => Rule(_ => Success(LPrLLP))
        case "05" => Rule(_ => Success(UnincorporatedBody))
        case _ =>
          Rule { _ =>
            Failure(Seq(Path \ "businessType" -> Seq(ValidationError("error.invalid"))))
          }
      }
    }

  implicit val formW: Write[BusinessType, UrlFormEncoded] =
    Write[BusinessType, UrlFormEncoded] {
      case LimitedCompany =>
        Map("businessType" -> Seq("01"))
      case SoleProprietor =>
        Map("businessType" -> Seq("02"))
      case Partnership =>
        Map("businessType" -> Seq("03"))
      case LPrLLP =>
        Map("businessType" -> Seq("04"))
      case UnincorporatedBody =>
        Map("businessType" -> Seq("05"))
    }

  implicit val writes = Writes[BusinessType] {
    case LimitedCompany => JsString("Corporate Body")
    case SoleProprietor => JsString("Sole Trader")
    case Partnership => JsString("Partnership")
    case LPrLLP => JsString("LLP")
    case UnincorporatedBody => JsString("Unincorporated Body")
  }

  implicit val reads = Reads[BusinessType] {
    case JsString("Corporate Body") => JsSuccess(LimitedCompany)
    case JsString("Sole Trader") => JsSuccess(SoleProprietor)
    case JsString("Partnership") => JsSuccess(Partnership)
    case JsString("LLP") => JsSuccess(LPrLLP)
    case JsString("Unincorporated Body") => JsSuccess(UnincorporatedBody)
    case _ =>
      JsError(JsPath -> ValidationError("error.invalid"))
  }
}
