package models.businessmatching

import jto.validation.forms.UrlFormEncoded
import jto.validation._
import play.api.libs.json._
import cats.data.Validated.{Invalid, Valid}

sealed trait BusinessType

object BusinessType {

  import jto.validation.forms.Rules._
  import utils.MappingUtils.Implicits._

  case object SoleProprietor extends BusinessType
  case object LimitedCompany extends BusinessType
  case object Partnership extends BusinessType
  case object LPrLLP extends BusinessType
  case object UnincorporatedBody extends BusinessType

  implicit val formR: Rule[UrlFormEncoded, BusinessType] =
    From[UrlFormEncoded] { __ =>
      (__ \ "businessType").read[String] flatMap {
        case "01" => Rule(_ => Valid(LimitedCompany))
        case "02" => Rule(_ => Valid(SoleProprietor))
        case "03" => Rule(_ => Valid(Partnership))
        case "04" => Rule(_ => Valid(LPrLLP))
        case "05" => Rule(_ => Valid(UnincorporatedBody))
        case _ =>
          Rule { _ =>
            Invalid(Seq(Path \ "businessType" -> Seq(ValidationError("error.invalid"))))
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
      JsError(JsPath -> play.api.data.validation.ValidationError("error.invalid"))
  }
}
