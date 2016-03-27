package models.businessactivities

import play.api.data.mapping.{Path, Write, From, Rule}
import play.api.data.mapping.forms._
import play.api.data.validation.ValidationError
import play.api.libs.json.Json

case class AccountantForAMLSRegulations(accountantForAMLSRegulations: Boolean)


object AccountantForAMLSRegulations {

  implicit val formats = Json.format[AccountantForAMLSRegulations]

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, AccountantForAMLSRegulations] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "accountantForAMLSRegulations").read[Option[Boolean]] flatMap {
      case Some(data) => AccountantForAMLSRegulations(data)
      case _ => Path \ "accountantForAMLSRegulations" -> Seq(ValidationError("error.required.ba.business.use.accountant"))
    }
  }

  implicit val formWrites: Write[AccountantForAMLSRegulations, UrlFormEncoded] = Write {
    case AccountantForAMLSRegulations(registered) => Map("accountantForAMLSRegulations" -> Seq(registered.toString))
  }

}