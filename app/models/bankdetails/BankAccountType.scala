package models.bankdetails

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait BankAccountType

case object PersonalAccount extends BankAccountType
case object BelongsToBusiness extends BankAccountType
case object BelongsToOtherBusiness extends BankAccountType
case object NoBankAccount extends BankAccountType

object BankAccountType {

  import utils.MappingUtils.Implicits._

  implicit val formReads:Rule[UrlFormEncoded, BankAccountType] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "bankAccountType").read[String] flatMap {
      case "01" => PersonalAccount
      case "02" => BelongsToBusiness
      case "03" => BelongsToOtherBusiness
      case "04" => NoBankAccount
      case _ =>
        (Path \ "bankAccountType") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites:Write[BankAccountType, UrlFormEncoded] = Write {
    case PersonalAccount => "bankAccountType" -> "01"
    case BelongsToBusiness => "bankAccountType" -> "02"
    case BelongsToOtherBusiness => "bankAccountType" -> "03"
    case NoBankAccount => "bankAccountType" -> "04"
  }

  implicit val jsonReads : Reads[BankAccountType] = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "bankAccountType").read[String] flatMap {
      case "01" => PersonalAccount
      case "02" => BelongsToBusiness
      case "03" => BelongsToOtherBusiness
      case "04" => NoBankAccount
      case _ =>
        ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[BankAccountType] {
    case PersonalAccount => Json.obj("bankAccountType"->"01")
    case BelongsToBusiness => Json.obj("bankAccountType" -> "02")
    case BelongsToOtherBusiness => Json.obj("bankAccountType" -> "03")
    case NoBankAccount => Json.obj("bankAccountType" -> "04")
  }

}
